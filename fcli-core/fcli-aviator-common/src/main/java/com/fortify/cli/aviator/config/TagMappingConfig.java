/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.aviator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;

import lombok.Data;

@Data @Reflectable
public class TagMappingConfig {
    public static final String SUPPRESSION_SELECTOR_CATEGORIES = "categories";

    private String tag_id = "87f2364f-dcd4-49e6-861d-f8d3f351686b";
    private List<SuppressionExclusion> suppression_exclusions = new ArrayList<>();
    private Mapping mapping;

    public void setSuppression_exclusions(List<SuppressionExclusion> suppression_exclusions) {
        this.suppression_exclusions = suppression_exclusions == null ? new ArrayList<>() : suppression_exclusions;
    }

    public void validate() {
        List<String> errors = new ArrayList<>();

        validateRequired(errors, "mapping", mapping);
        if (mapping != null) {
            validateTier(errors, "mapping.tier_1", mapping.getTier_1());
            validateTier(errors, "mapping.tier_2", mapping.getTier_2());
        }

        validateSuppressionExclusions(errors);

        if (!errors.isEmpty()) {
            throw new AviatorSimpleException("Invalid tag mapping configuration: " + String.join("; ", errors));
        }

        for (SuppressionExclusion exclusion : suppression_exclusions) {
            if (exclusion != null) {
                exclusion.initNormalized();
            }
        }
    }

    public boolean hasSuppressionExclusions() {
        return suppression_exclusions != null
                && suppression_exclusions.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(SuppressionExclusion::hasSupportedSelectors);
    }

    public boolean requiresCategoryForSuppressionEvaluation() {
        return suppression_exclusions != null
                && suppression_exclusions.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(exclusion -> exclusion.usesSelector(SUPPRESSION_SELECTOR_CATEGORIES));
    }

    public boolean isSuppressionExcluded(SuppressionExclusionContext context) {
        if (context == null || suppression_exclusions == null) {
            return false;
        }
        return suppression_exclusions.stream()
                .filter(Objects::nonNull)
                .anyMatch(exclusion -> exclusion.matches(context));
    }

    private void validateTier(List<String> errors, String path, Tier tier) {
        validateRequired(errors, path, tier);
        if (tier != null) {
            validateRequired(errors, path + ".fp", tier.getFp());
            validateRequired(errors, path + ".tp", tier.getTp());
            validateRequired(errors, path + ".unsure", tier.getUnsure());
        }
    }

    private void validateSuppressionExclusions(List<String> errors) {
        if (suppression_exclusions == null) {
            return;
        }

        for (int i = 0; i < suppression_exclusions.size(); i++) {
            SuppressionExclusion suppressionExclusion = suppression_exclusions.get(i);
            if (suppressionExclusion == null) {
                errors.add("suppression_exclusions[" + i + "] must be a non-null object");
                continue;
            }
            suppressionExclusion.validate(errors, i);
        }
    }

    private void validateRequired(List<String> errors, String path, Object value) {
        if (value == null) {
            errors.add(path + " is required");
        }
    }

    private static String normalizeSelectorValue(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record SuppressionExclusionContext(Map<String, Set<String>> exactMatchSelectorValues) {
        public SuppressionExclusionContext {
            exactMatchSelectorValues = exactMatchSelectorValues == null
                    ? Collections.emptyMap()
                    : exactMatchSelectorValues;
        }

        public SuppressionExclusionContext(String category) {
            this(builder()
                    .withExactMatchSelectorValue(SUPPRESSION_SELECTOR_CATEGORIES, category)
                    .build()
                    .exactMatchSelectorValues());
        }

        Set<String> getExactMatchSelectorValues(String selectorName) {
            return exactMatchSelectorValues.getOrDefault(selectorName, Collections.emptySet());
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final Map<String, Set<String>> exactMatchSelectorValues = new LinkedHashMap<>();

            public Builder withExactMatchSelectorValue(String selectorName, String value) {
                if (selectorName != null && value != null && !value.isBlank()) {
                    exactMatchSelectorValues
                            .computeIfAbsent(selectorName, unused -> new LinkedHashSet<>())
                            .add(normalizeSelectorValue(value));
                }
                return this;
            }

            public SuppressionExclusionContext build() {
                if (exactMatchSelectorValues.isEmpty()) {
                    return new SuppressionExclusionContext(Collections.emptyMap());
                }

                Map<String, Set<String>> normalizedSelectorValues = new LinkedHashMap<>();
                exactMatchSelectorValues.forEach((selectorName, values) -> normalizedSelectorValues.put(
                        selectorName,
                        values.isEmpty()
                                ? Collections.emptySet()
                                : Collections.unmodifiableSet(new LinkedHashSet<>(values))));
                return new SuppressionExclusionContext(Collections.unmodifiableMap(normalizedSelectorValues));
            }
        }
    }

    @Data @Reflectable
    public static class SuppressionExclusion {
        private List<String> categories = new ArrayList<>();
        private transient Set<String> normalizedCategories;

        public void setCategories(List<String> categories) {
            this.categories = categories == null ? new ArrayList<>() : categories;
            this.normalizedCategories = null;
        }

        void initNormalized() {
            normalizedCategories = buildNormalizedCategories();
        }

        boolean hasSupportedSelectors() {
            return usesSelector(SUPPRESSION_SELECTOR_CATEGORIES);
        }

        boolean usesSelector(String selectorName) {
            return SUPPRESSION_SELECTOR_CATEGORIES.equals(selectorName)
                    && categories != null
                    && !categories.isEmpty();
        }

        boolean matches(SuppressionExclusionContext context) {
            if (!hasSupportedSelectors()) {
                return false;
            }
            return matchesExactMatchSelector(SUPPRESSION_SELECTOR_CATEGORIES, getNormalizedCategories(), context);
        }

        private boolean matchesExactMatchSelector(String selectorName, Set<String> configuredValues,
                SuppressionExclusionContext context) {
            if (!usesSelector(selectorName)) {
                return true;
            }

            Set<String> contextValues = context.getExactMatchSelectorValues(selectorName);
            if (contextValues.isEmpty()) {
                return false;
            }
            return contextValues.stream().anyMatch(configuredValues::contains);
        }

        private Set<String> getNormalizedCategories() {
            if (normalizedCategories == null) {
                normalizedCategories = buildNormalizedCategories();
            }
            return normalizedCategories;
        }

        private Set<String> buildNormalizedCategories() {
            if (categories == null || categories.isEmpty()) {
                return Collections.emptySet();
            }
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (String category : categories) {
                if (category != null && !category.isBlank()) {
                    result.add(normalizeSelectorValue(category));
                }
            }
            return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
        }

        void validate(List<String> errors, int index) {
            if (!hasSupportedSelectors()) {
                errors.add("suppression_exclusions[" + index + "] must define at least one supported selector (e.g. categories)");
                return;
            }
            validateCategories(errors, index);
        }

        private void validateCategories(List<String> errors, int exclusionIndex) {
            if (categories == null || categories.isEmpty()) {
                return;
            }
            for (int j = 0; j < categories.size(); j++) {
                String category = categories.get(j);
                if (category == null || category.isBlank()) {
                    errors.add("suppression_exclusions[" + exclusionIndex + "].categories[" + j + "] must be a non-blank string");
                }
            }
        }
    }

    @Data @Reflectable
    public static class Mapping {
        private Tier tier_1;
        private Tier tier_2;
    }

    @Data @Reflectable
    public static class Tier {
        private Result fp;
        private Result tp;
        private Result unsure;
    }

    @Data @Reflectable
    public static class Result {
        private String value;
        private Boolean suppress = false;
    }
}