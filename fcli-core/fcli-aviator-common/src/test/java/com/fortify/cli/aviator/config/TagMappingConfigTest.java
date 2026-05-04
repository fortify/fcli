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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.util.ResourceUtil;

class TagMappingConfigTest {
    @TempDir Path tempDir;

    @Test
    void testValidateRejectsBlankSuppressionExclusionCategories() {
        TagMappingConfig config = createValidConfig();
        config.setSuppression_exclusions(new ArrayList<>(List.of(createSuppressionExclusion("Privacy Violation", "  "))));

        AviatorSimpleException exception = assertThrows(AviatorSimpleException.class, config::validate);

        assertEquals(
                "Invalid tag mapping configuration: suppression_exclusions[0].categories[1] must be a non-blank string",
                exception.getMessage());
    }

    @Test
    void testValidateRejectsSuppressionExclusionWithoutCategories() {
        TagMappingConfig config = createValidConfig();
        config.setSuppression_exclusions(new ArrayList<>(List.of(new TagMappingConfig.SuppressionExclusion())));

        AviatorSimpleException exception = assertThrows(AviatorSimpleException.class, config::validate);

        assertEquals(
                "Invalid tag mapping configuration: suppression_exclusions[0] must define at least one supported selector (e.g. categories)",
                exception.getMessage());
    }

    @Test
    void testSuppressionExclusionsMatchingIsCaseInsensitiveAndExact() {
        TagMappingConfig config = createValidConfig();
        config.setSuppression_exclusions(new ArrayList<>(List.of(createSuppressionExclusion("Privacy Violation"))));
        config.validate();

        assertTrue(config.hasSuppressionExclusions());
        assertTrue(config.isSuppressionExcluded(new TagMappingConfig.SuppressionExclusionContext("privacy violation")));
        assertTrue(config.isSuppressionExcluded(new TagMappingConfig.SuppressionExclusionContext(" Privacy Violation ")));
        assertFalse(config.isSuppressionExcluded(new TagMappingConfig.SuppressionExclusionContext("Privacy")));
    }

    @Test
    void testSuppressionExclusionContextBuilderMatchesConfiguredSelectors() {
        TagMappingConfig config = createValidConfig();
        config.setSuppression_exclusions(new ArrayList<>(List.of(createSuppressionExclusion("Privacy Violation"))));
        config.validate();

        TagMappingConfig.SuppressionExclusionContext context = TagMappingConfig.SuppressionExclusionContext.builder()
                .withExactMatchSelectorValue(TagMappingConfig.SUPPRESSION_SELECTOR_CATEGORIES, " privacy violation ")
                .build();

        assertTrue(config.isSuppressionExcluded(context));
    }

    @Test
    void testSuppressionExclusionCategoryCacheIsInvalidatedOnMutation() {
        TagMappingConfig config = createValidConfig();
        TagMappingConfig.SuppressionExclusion suppressionExclusion = createSuppressionExclusion("Privacy Violation");
        config.setSuppression_exclusions(new ArrayList<>(List.of(suppressionExclusion)));
        config.validate();

        assertTrue(config.isSuppressionExcluded(new TagMappingConfig.SuppressionExclusionContext("privacy violation")));

        suppressionExclusion.setCategories(new ArrayList<>(List.of("SQL Injection")));

        assertFalse(config.isSuppressionExcluded(new TagMappingConfig.SuppressionExclusionContext("privacy violation")));
        assertTrue(config.isSuppressionExcluded(new TagMappingConfig.SuppressionExclusionContext("sql injection")));
    }

    @Test
    void testLoadYamlFileBindsSuppressionExclusionsAcrossEntries() throws Exception {
        Path yamlFile = tempDir.resolve("tag-mapping.yaml");
        Files.writeString(yamlFile, """
                tag_id: \"87f2364f-dcd4-49e6-861d-f8d3f351686b\"
                suppression_exclusions:
                  - categories:
                      - \"Privacy Violation\"
                  - categories:
                      - \"SQL Injection\"
                      - \"privacy violation\"
                mapping:
                  tier_1:
                    fp:
                      value: \"Not an Issue\"
                      suppress: true
                    tp:
                      value: \"Exploitable\"
                      suppress: false
                    unsure:
                      suppress: false
                  tier_2:
                    fp:
                      value: \"Not an Issue\"
                      suppress: false
                    tp:
                      value: \"Suspicious\"
                      suppress: false
                    unsure:
                      suppress: false
                """);

        TagMappingConfig config = ResourceUtil.loadYamlFile(yamlFile.toFile(), TagMappingConfig.class);
        config.validate();

        assertTrue(config.hasSuppressionExclusions());
        assertTrue(config.isSuppressionExcluded(new TagMappingConfig.SuppressionExclusionContext("SQL Injection")));
        assertTrue(config.isSuppressionExcluded(new TagMappingConfig.SuppressionExclusionContext("privacy violation")));
    }

    private TagMappingConfig createValidConfig() {
        TagMappingConfig config = new TagMappingConfig();
        TagMappingConfig.Mapping mapping = new TagMappingConfig.Mapping();
        mapping.setTier_1(createTier(true));
        mapping.setTier_2(createTier(false));
        config.setMapping(mapping);
        return config;
    }

    private TagMappingConfig.Tier createTier(boolean suppressFalsePositives) {
        TagMappingConfig.Tier tier = new TagMappingConfig.Tier();
        tier.setFp(createResult("Not an Issue", suppressFalsePositives));
        tier.setTp(createResult("Exploitable", false));
        tier.setUnsure(createResult(null, false));
        return tier;
    }

    private TagMappingConfig.Result createResult(String value, boolean suppress) {
        TagMappingConfig.Result result = new TagMappingConfig.Result();
        result.setValue(value);
        result.setSuppress(suppress);
        return result;
    }

    private TagMappingConfig.SuppressionExclusion createSuppressionExclusion(String... categories) {
        TagMappingConfig.SuppressionExclusion suppressionExclusion = new TagMappingConfig.SuppressionExclusion();
        suppressionExclusion.setCategories(new ArrayList<>(List.of(categories)));
        return suppressionExclusion;
    }
}
