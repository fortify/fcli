package com.fortify.cli.aviator.fpr.utils;

import java.util.regex.Pattern;

/**
 * Interface for objects that can be searched for string matches or patterns.
 * Used in description conditionals (e.g., IfDef, ConditionalText) or filtering.
 */
public interface Searchable {
    /**
     * Checks if the object contains the given string in its searchable content.
     *
     * @param searchString String to search for
     * @return true if the string is found, false otherwise
     */
    boolean contains(String searchString);

    /**
     * Checks if the object exactly matches the given string in its searchable content.
     *
     * @param matchString String to match exactly
     * @return true if exact match found, false otherwise
     */
    boolean matches(String matchString);

    /**
     * Checks if the object matches the given regex pattern in its searchable content.
     *
     * @param pattern Regex pattern to match
     * @return true if pattern matches, false otherwise
     */
    boolean matchesPattern(Pattern pattern);
}