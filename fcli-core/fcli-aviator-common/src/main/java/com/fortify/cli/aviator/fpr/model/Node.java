package com.fortify.cli.aviator.fpr.model;

import com.fortify.cli.aviator.fpr.utils.Searchable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Represents a node in the FVDL UnifiedNodePool or trace entry.
 * Enhanced with taintFlags, knowledge, secondary location, detailsOnly, label, and reasonText for full coverage.
 * Implements Searchable for description conditionals.
 */
@Getter
@Setter
@AllArgsConstructor
public class Node implements Searchable {
    private String id;
    private String filePath;
    private int line;
    private int lineEnd;
    private int colStart;
    private int colEnd;
    private String contextId;
    private String snippet;
    private String actionType;
    private String additionalInfo;
    private String associatedRuleId;

    private List<String> taintFlags = new ArrayList<>(); // From Knowledge/Fact type="TaintFlags"
    private Map<String, String> knowledge = new HashMap<>(); // From Knowledge/Fact (type -> value)

    private String secondaryPath = ""; // From SecondaryLocation
    private int secondaryLine = 0;
    private int secondaryLineEnd = 0;
    private int secondaryColStart = 0;
    private int secondaryColEnd = 0;
    private String secondaryContextId = "";
    private boolean detailsOnly = false; // From @detailsOnly
    private String label = ""; // From @label
    private String reasonText = ""; // From Reason/Internal or concatenated reason info

    /**
     * Checks if any searchable field contains the given string.
     *
     * @param searchString String to search for
     * @return true if found in searchable fields
     */
    @Override
    public boolean contains(String searchString) {
        if (searchString == null || searchString.isEmpty()) {
            return false;
        }
        String lowerSearch = searchString.toLowerCase();
        return (filePath != null && filePath.toLowerCase().contains(lowerSearch))
                || (secondaryPath != null && secondaryPath.toLowerCase().contains(lowerSearch))
                || (actionType != null && actionType.toLowerCase().contains(lowerSearch))
                || (additionalInfo != null && additionalInfo.toLowerCase().contains(lowerSearch))
                || (associatedRuleId != null && associatedRuleId.toLowerCase().contains(lowerSearch))
                || (reasonText != null && reasonText.toLowerCase().contains(lowerSearch))
                || (label != null && label.toLowerCase().contains(lowerSearch))
                || taintFlags.stream().anyMatch(flag -> flag.toLowerCase().contains(lowerSearch))
                || knowledge.values().stream().anyMatch(value -> value != null && value.toLowerCase().contains(lowerSearch));
    }

    /**
     * Checks if any searchable field exactly matches the given string.
     *
     * @param matchString String to match exactly
     * @return true if exact match in searchable fields
     */
    @Override
    public boolean matches(String matchString) {
        if (matchString == null || matchString.isEmpty()) {
            return false;
        }
        String lowerMatch = matchString.toLowerCase();
        return (filePath != null && filePath.toLowerCase().equals(lowerMatch))
                || (secondaryPath != null && secondaryPath.toLowerCase().equals(lowerMatch))
                || (actionType != null && actionType.toLowerCase().equals(lowerMatch))
                || (additionalInfo != null && additionalInfo.toLowerCase().equals(lowerMatch))
                || (associatedRuleId != null && associatedRuleId.toLowerCase().equals(lowerMatch))
                || (reasonText != null && reasonText.toLowerCase().equals(lowerMatch))
                || (label != null && label.toLowerCase().equals(lowerMatch))
                || taintFlags.stream().anyMatch(flag -> flag.toLowerCase().equals(lowerMatch))
                || knowledge.values().stream().anyMatch(value -> value != null && value.toLowerCase().equals(lowerMatch));
    }

    /**
     * Checks if any searchable field matches the given regex pattern.
     *
     * @param pattern Regex pattern to match
     * @return true if pattern matches searchable fields
     */
    @Override
    public boolean matchesPattern(Pattern pattern) {
        if (pattern == null) {
            return false;
        }
        return (filePath != null && pattern.matcher(filePath).matches())
                || (secondaryPath != null && pattern.matcher(secondaryPath).matches())
                || (actionType != null && pattern.matcher(actionType).matches())
                || (additionalInfo != null && pattern.matcher(additionalInfo).matches())
                || (associatedRuleId != null && pattern.matcher(associatedRuleId).matches())
                || (reasonText != null && pattern.matcher(reasonText).matches())
                || (label != null && pattern.matcher(label).matches())
                || taintFlags.stream().anyMatch(flag -> pattern.matcher(flag).matches())
                || knowledge.values().stream().anyMatch(value -> value != null && pattern.matcher(value).matches());
    }
}