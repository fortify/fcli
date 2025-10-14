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