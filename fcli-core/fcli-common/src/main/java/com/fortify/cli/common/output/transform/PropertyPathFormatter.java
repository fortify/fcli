/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.transform;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PropertyPathFormatter {
    public static final String humanReadable(String propertyPath) {
        String normalizedWithSpaces = normalize(propertyPath).replace('.', ' ');
        return capitalize(normalizedWithSpaces);
    }
    
    public static final String snakeCase(String propertyPath) {
        return normalize(propertyPath).replace('.', '_');
    }
    
    public static final String pascalCase(String propertyPath) {
        String[] elts = normalize(propertyPath).split("\\.");
        return Stream.of(elts).map(PropertyPathFormatter::capitalize).collect(Collectors.joining());
    }
    
    public static final String camelCase(String propertyPath) {
        String pascalCase = pascalCase(propertyPath);
        return pascalCase.substring(0, 1).toLowerCase() + pascalCase.substring(1);
    }
    
    private static final String normalize(String s) {
        return s
                .replaceAll("_", ".")             // Underscore to dot
                .replaceAll("[^a-zA-Z0-9. ]", "") // Remove all special characters
                .replaceAll("([A-Z]+)", ".$1")    // Insert dot before uppercase words
                .replaceAll("\\.\\.", ".")        // Remove any duplicate dots
                .replaceAll("^\\.+", "")          // Remove leading dots
                .replaceAll("\\.+$", "")          // Remove trailing dots
                .toLowerCase();
    }
    
    private static final String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}