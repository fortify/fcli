/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.fpr.utils;

import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for search operations, such as string contains/matches and pattern matching.
 * Used in description conditionals or other search-related logic if needed.
 */
public class SearchUtils {
    private static final Logger logger = LoggerFactory.getLogger(SearchUtils.class);

    /**
     * Checks if a map contains a string in any of its values.
     *
     * @param match String to search for
     * @param map   Map with Searchable values
     * @return true if match found in any value
     */
    public static boolean mapContainsString(String match, Map<String, ? extends Searchable> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }
        for (Searchable searchable : map.values()) {
            if (searchable != null && searchable.contains(match)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a map exactly matches a string in any of its values.
     *
     * @param match String to match
     * @param map   Map with Searchable values
     * @return true if exact match found in any value
     */
    public static boolean mapMatchesString(String match, Map<String, ? extends Searchable> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }
        for (Searchable searchable : map.values()) {
            if (searchable != null && searchable.matches(match)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a map matches a pattern in any of its values.
     *
     * @param regex Pattern to match
     * @param map   Map with Searchable values
     * @return true if pattern matches any value
     */
    public static boolean mapMatchesPattern(Pattern regex, Map<String, ? extends Searchable> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }
        for (Searchable searchable : map.values()) {
            if (searchable != null && searchable.matchesPattern(regex)) {
                return true;
            }
        }
        return false;
    }
}