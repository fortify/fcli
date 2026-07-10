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
package com.fortify.cli.aviator.ssc.helper;

import java.util.*;

/**
 * Manages category equivalence classes for SAST-DAST correlation.
 *
 * In most cases, SAST and DAST use the same category names. However, in some cases,
 * different category names may refer to the same underlying vulnerability type.
 * This class allows defining equivalence classes of categories that should be
 * treated as matching candidates during correlation.
 *
 * Example: "Cross-Frame Scripting" and "HTML5: Missing Frame Protection" are
 * different names for related vulnerabilities and should be considered as
 * correlation candidates.
 */
public class CategoryEquivalence {

    /**
     * List of equivalence classes. Each set contains categories that should be
     * treated as equivalent for correlation purposes.
     */
    private static final List<Set<String>> EQUIVALENCE_CLASSES = new ArrayList<>();

    /**
     * Map from each category to its canonical (normalized) form.
     * Categories not in any equivalence class map to themselves.
     */
    private static final Map<String, String> CANONICAL_MAP = new HashMap<>();

    static {
        // Define equivalence classes here.
        // Add new equivalence classes as they are discovered.

        // Frame protection related vulnerabilities
        addEquivalenceClass(
            "Cross-Frame Scripting",
            "HTML5: Missing Framing Protection"
        );

        // Add more equivalence classes below as needed:
        // addEquivalenceClass("Category A", "Category B", "Category C");
    }

    /**
     * Adds an equivalence class. All provided categories will be treated as
     * equivalent for correlation purposes.
     *
     * @param categories Two or more category names that should be considered equivalent
     */
    private static void addEquivalenceClass(String... categories) {
        if (categories.length < 2) {
            return;
        }

        Set<String> equivalenceClass = Set.of(categories);
        EQUIVALENCE_CLASSES.add(equivalenceClass);

        // Use the first category as the canonical form
        String canonical = categories[0];
        for (String category : categories) {
            CANONICAL_MAP.put(category, canonical);
        }
    }

    /**
     * Gets the canonical (normalized) form of a category.
     *
     * For categories that are part of an equivalence class, this returns the
     * canonical representative of that class. For other categories, it returns
     * the category itself.
     *
     * @param category The category to normalize
     * @return The canonical form of the category
     */
    public static String getCanonical(String category) {
        if (category == null) {
            return null;
        }
        return CANONICAL_MAP.getOrDefault(category, category);
    }

    /**
     * Checks if two categories are equivalent (either identical or in the same
     * equivalence class).
     *
     * @param category1 First category
     * @param category2 Second category
     * @return true if the categories are equivalent
     */
    public static boolean areEquivalent(String category1, String category2) {
        if (category1 == null || category2 == null) {
            return false;
        }
        if (category1.equals(category2)) {
            return true;
        }
        String canonical1 = getCanonical(category1);
        String canonical2 = getCanonical(category2);
        return canonical1.equals(canonical2);
    }

    /**
     * Checks if a category is part of any equivalence class.
     *
     * @param category The category to check
     * @return true if the category is part of an equivalence class
     */
    public static boolean hasEquivalentCategories(String category) {
        return category != null && CANONICAL_MAP.containsKey(category);
    }

    /**
     * Gets all categories that are equivalent to the given category.
     * Returns a set containing at least the input category itself.
     *
     * @param category The category to find equivalents for
     * @return Set of equivalent categories (including the input category)
     */
    public static Set<String> getEquivalentCategories(String category) {
        if (category == null) {
            return Set.of();
        }

        for (Set<String> equivalenceClass : EQUIVALENCE_CLASSES) {
            if (equivalenceClass.contains(category)) {
                return equivalenceClass;
            }
        }

        return Set.of(category);
    }
}
