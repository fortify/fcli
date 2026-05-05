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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CategoryEquivalenceTest {

    @Test
    void testGetCanonical_equivalentCategory() {
        String canonical = CategoryEquivalence.getCanonical("HTML5: Missing Framing Protection");
        assertEquals("Cross-Frame Scripting", canonical,
            "Equivalent category should map to the canonical form");
    }

    @Test
    void testGetCanonical_alreadyCanonical() {
        String canonical = CategoryEquivalence.getCanonical("Cross-Frame Scripting");
        assertEquals("Cross-Frame Scripting", canonical);
    }

    @Test
    void testGetCanonical_unknownCategory() {
        String canonical = CategoryEquivalence.getCanonical("SQL Injection");
        assertEquals("SQL Injection", canonical,
            "Unknown category should map to itself");
    }

    @Test
    void testGetCanonical_null() {
        assertNull(CategoryEquivalence.getCanonical(null));
    }

    @Test
    void testAreEquivalent_sameCategory() {
        assertTrue(CategoryEquivalence.areEquivalent("SQL Injection", "SQL Injection"));
    }

    @Test
    void testAreEquivalent_equivalentCategories() {
        assertTrue(CategoryEquivalence.areEquivalent(
            "Cross-Frame Scripting", "HTML5: Missing Framing Protection"));
    }

    @Test
    void testAreEquivalent_differentCategories() {
        assertFalse(CategoryEquivalence.areEquivalent("SQL Injection", "Cross-Site Scripting"));
    }

    @Test
    void testAreEquivalent_nullInput() {
        assertFalse(CategoryEquivalence.areEquivalent(null, "SQL Injection"));
        assertFalse(CategoryEquivalence.areEquivalent("SQL Injection", null));
        assertFalse(CategoryEquivalence.areEquivalent(null, null));
    }

    @Test
    void testHasEquivalentCategories_true() {
        assertTrue(CategoryEquivalence.hasEquivalentCategories("Cross-Frame Scripting"));
        assertTrue(CategoryEquivalence.hasEquivalentCategories("HTML5: Missing Framing Protection"));
    }

    @Test
    void testHasEquivalentCategories_false() {
        assertFalse(CategoryEquivalence.hasEquivalentCategories("SQL Injection"));
    }

    @Test
    void testHasEquivalentCategories_null() {
        assertFalse(CategoryEquivalence.hasEquivalentCategories(null));
    }

    @Test
    void testGetEquivalentCategories_known() {
        Set<String> equivalents = CategoryEquivalence.getEquivalentCategories("Cross-Frame Scripting");
        assertEquals(2, equivalents.size());
        assertTrue(equivalents.contains("Cross-Frame Scripting"));
        assertTrue(equivalents.contains("HTML5: Missing Framing Protection"));
    }

    @Test
    void testGetEquivalentCategories_unknown() {
        Set<String> equivalents = CategoryEquivalence.getEquivalentCategories("SQL Injection");
        assertEquals(1, equivalents.size());
        assertTrue(equivalents.contains("SQL Injection"));
    }

    @Test
    void testGetEquivalentCategories_null() {
        Set<String> equivalents = CategoryEquivalence.getEquivalentCategories(null);
        assertTrue(equivalents.isEmpty());
    }
}
