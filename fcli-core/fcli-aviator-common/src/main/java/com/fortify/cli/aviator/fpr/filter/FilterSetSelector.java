package com.fortify.cli.aviator.fpr.filter;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public final class FilterSetSelector {
    private static final Logger LOG = LoggerFactory.getLogger(FilterSetSelector.class);

    private FilterSetSelector() {}

    /**
     * Selects the active FilterSet based on a prioritized set of user inputs.
     *
     * @param fprInfo           The fully parsed FPR information.
     * @param priorities        A list of priority levels from the --priority flag.
     * @param filterSetNameOrId A name or ID from the --filter-set flag.
     * @param ignoreFilters     A boolean from the --ignore-filters flag.
     * @return The single FilterSet to be applied, or null if none should be.
     * @throws AviatorSimpleException if a specified filter set cannot be found or a priority is invalid.
     */
    public static FilterSet select(FPRInfo fprInfo, List<String> priorities, String filterSetNameOrId, boolean ignoreFilters) {
        if (priorities != null && !priorities.isEmpty()) {
            LOG.info("User has specified a priority filter for: {}", priorities);
            return createPriorityFilterSet(priorities);
        }

        if (ignoreFilters) {
            LOG.info("User has chosen to ignore all filter sets.");
            return null;
        }

        if (filterSetNameOrId != null && !filterSetNameOrId.trim().isEmpty()) {
            LOG.info("Attempting to find user-specified filter set by name or ID: '{}'", filterSetNameOrId);
            return findFilterSetByNameOrId(fprInfo, filterSetNameOrId);
        }

        FilterSet defaultSet = fprInfo.getDefaultEnabledFilterSet().orElse(null);
        if (defaultSet != null) {
            LOG.info("Using default enabled filter set found in FPR: '{}'", defaultSet.getTitle());
        } else {
            LOG.info("No active filter set specified or found. Auditing all applicable issues.");
        }
        return defaultSet;
    }

    /**
     * Finds a FilterSet by a regex/wildcard name, a partial name, or an exact ID match.
     * If no match is found, it throws a helpful exception listing available filter sets.
     *
     * @param fprInfo  The fully parsed FPR information.
     * @param nameOrId The user-provided name (can be a regex) or full ID.
     * @return The single matching FilterSet.
     * @throws AviatorSimpleException if no filter sets are available, no match is found, or the match is ambiguous.
     */
    private static FilterSet findFilterSetByNameOrId(FPRInfo fprInfo, String nameOrId) {
        if (fprInfo.getFilterTemplate() == null || fprInfo.getFilterTemplate().getFilterSets().isEmpty()) {
            throw new AviatorSimpleException("A filter set was specified, but the FPR does not contain a filtertemplate.xml or it has no filter sets defined.");
        }

        List<FilterSet> allFilterSets = fprInfo.getFilterTemplate().getFilterSets();
        String trimmedInput = nameOrId.trim();
        String lowerInput = trimmedInput.toLowerCase();


        // 1. First, always try for an exact ID match. This is the most specific.
        Optional<FilterSet> matchById = allFilterSets.stream()
                .filter(fs -> fs.getId().equalsIgnoreCase(trimmedInput))
                .findFirst();

        if (matchById.isPresent()) {
            return matchById.get();
        }

        List<FilterSet> matchesByName;

        // 2. Check if the input looks like a regex/wildcard.
        if (trimmedInput.matches(".*[\\*\\?\\.\\+\\^\\$\\|\\\\(\\)\\[\\]\\{\\}].*")) {
            try {
                // It looks like a regex, so we'll use regex matching.
                LOG.debug("Interpreting filter set name '{}' as a regular expression.", trimmedInput);
                Pattern pattern = Pattern.compile(trimmedInput, Pattern.CASE_INSENSITIVE);
                matchesByName = allFilterSets.stream()
                        .filter(fs -> pattern.matcher(fs.getTitle()).matches())
                        .collect(Collectors.toList());
            } catch (PatternSyntaxException e) {
                throw new AviatorSimpleException("Invalid regular expression provided for filter set name: " + e.getMessage());
            }
        } else {
            // 3. If not a regex, fall back to the user-friendly "starts with" matching.
            matchesByName = allFilterSets.stream()
                    .filter(fs -> fs.getTitle().toLowerCase().startsWith(lowerInput))
                    .collect(Collectors.toList());
        }

        // 4. Handle the results and provide helpful error messages.
        if (matchesByName.size() == 1) {
            return matchesByName.get(0);
        }

        String availableTitles = allFilterSets.stream()
                .map(FilterSet::getTitle)
                .collect(Collectors.joining("', '", "'", "'"));

        if (matchesByName.isEmpty()) {
            throw new AviatorSimpleException(
                    String.format("Filter set '%s' not found. Available filter sets are: %s", nameOrId, availableTitles)
            );
        } else {
            String ambiguousTitles = matchesByName.stream()
                    .map(FilterSet::getTitle)
                    .collect(Collectors.joining("', '", "'", "'"));
            throw new AviatorSimpleException(
                    String.format("Filter set pattern '%s' is ambiguous. Did you mean one of: %s", nameOrId, ambiguousTitles)
            );
        }
    }

    /**
     * Creates a new FilterSet in memory based on a list of priority levels.
     * This version resolves abbreviations and simple wildcards (*).
     *
     * @param priorities A list of user-provided priority strings (e.g., "c", "h*", "medium").
     * @return A dynamically generated FilterSet.
     * @throws AviatorSimpleException if a provided priority is ambiguous or invalid.
     */
    private static FilterSet createPriorityFilterSet(List<String> priorities) {
        // Define the canonical list of valid priorities for matching.
        final List<String> validPriorities = List.of("Critical", "High", "Medium", "Low");

        // Use a Set to store resolved priorities to handle duplicates automatically
        Set<String> resolvedPriorities = new HashSet<>();

        for (String userInput : priorities) {
            String trimmedInput = userInput.trim().toLowerCase();
            List<String> matches;

            if (trimmedInput.contains("*")) {
                final Pattern pattern = Pattern.compile(trimmedInput.replace("*", ".*"), Pattern.CASE_INSENSITIVE);
                matches = validPriorities.stream()
                        .filter(p -> pattern.matcher(p).matches())
                        .collect(Collectors.toList());
                if (matches.isEmpty()) {
                    throw new AviatorSimpleException("Priority wildcard '" + userInput + "' did not match any valid priorities: " + validPriorities);
                }
            } else {
                matches = validPriorities.stream()
                        .filter(p -> p.toLowerCase().startsWith(trimmedInput))
                        .collect(Collectors.toList());

                if (matches.isEmpty()) {
                    throw new AviatorSimpleException("Invalid priority value: '" + userInput + "'. Valid values are: " + validPriorities);
                }
                if (matches.stream().distinct().count() > 1) {
                    if (!trimmedInput.equalsIgnoreCase(matches.get(0))) { // Check if it's not a full name match
                        throw new AviatorSimpleException("Ambiguous priority value: '" + userInput + "'. It could mean any of: " + matches);
                    }
                }
            }
            resolvedPriorities.addAll(matches);
        }

        if (resolvedPriorities.isEmpty()) {
            LOG.warn("Priority filter was provided, but no matching priorities were found. No issues will be audited.");
            // Return a filter that matches nothing.
            return createPriorityFilterSet(List.of("THIS_WILL_NEVER_MATCH"));
        }

        FilterSet dynamicFilterSet = new FilterSet();
        dynamicFilterSet.setTitle("Dynamic Priority Filter: " + String.join(", ", resolvedPriorities));

        // Build the final, clean query string using the fully resolved priority names.
        String query = resolvedPriorities.stream()
                .map(p -> String.format("[fortify priority order]:%s", p))
                .collect(Collectors.joining(" OR "));

        Filter priorityFilter = new Filter();
        priorityFilter.setAction("setFolder");
        priorityFilter.setQuery(query);

        dynamicFilterSet.setFilters(List.of(priorityFilter));
        return dynamicFilterSet;
    }
}