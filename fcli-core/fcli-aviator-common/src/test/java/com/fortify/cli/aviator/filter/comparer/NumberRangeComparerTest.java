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
package com.fortify.cli.aviator.filter.comparer;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.filter.comparer.NumberRangeComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.SearchComparer;

class NumberRangeComparerTest {

    @Test
    void testInclusiveBrackets() {
        SearchComparer comparer = new NumberRangeComparer("[1,5]");
        assertTrue(comparer.matches(new BigDecimal("1.0")), "Should match lower bound");
        assertTrue(comparer.matches(new BigDecimal("5")), "Should match upper bound");
        assertTrue(comparer.matches(3), "Should match value inside");
        assertFalse(comparer.matches(0.9), "Should not match value below");
        assertFalse(comparer.matches(5.1), "Should not match value above");
    }

    @Test
    void testExclusiveParentheses() {
        SearchComparer comparer = new NumberRangeComparer("(1,5)");
        assertFalse(comparer.matches(1), "Should not match lower bound");
        assertFalse(comparer.matches(5), "Should not match upper bound");
        assertTrue(comparer.matches(3.5), "Should match value inside");
    }

    @Test
    void testMixedInclusiveExclusiveBrackets() {
        SearchComparer comparer1 = new NumberRangeComparer("[1,5)");
        assertTrue(comparer1.matches(1), "[1,5) should match lower bound");
        assertFalse(comparer1.matches(5), "[1,5) should not match upper bound");

        SearchComparer comparer2 = new NumberRangeComparer("(1,5]");
        assertFalse(comparer2.matches(1), "(1,5] should not match lower bound");
        assertTrue(comparer2.matches(5), "(1,5] should match upper bound");
    }

    @Test
    void testFloatingPointNumbers() {
        SearchComparer comparer = new NumberRangeComparer("[2.5, 4.6]");
        assertTrue(comparer.matches(2.5));
        assertTrue(comparer.matches(4.6));
        assertTrue(comparer.matches(3.0));
        assertFalse(comparer.matches(2.49));
        assertFalse(comparer.matches(4.61));
    }

    @Test
    void testCommaAndHyphenSeparators() {
        SearchComparer comma = new NumberRangeComparer("[1,5]");
        SearchComparer hyphen = new NumberRangeComparer("[1-5]");
        assertTrue(comma.matches(3), "Comma separator should work");
        assertTrue(hyphen.matches(3), "Hyphen separator should work");
    }

    @Test
    void testExtraWhitespace() {
        SearchComparer comparer = new NumberRangeComparer("  [ 1 , 5 ]  ");
        assertTrue(comparer.matches(2));
    }

    @Test
    void testSingleValueExactMatch() {
        SearchComparer comparer = new NumberRangeComparer("[3,3]");
        assertTrue(comparer.matches(3));
        assertFalse(comparer.matches(2.9));
        assertFalse(comparer.matches(3.1));
    }

    @Test
    void testThrowsExceptionOnMalformedRangeMissingEnd() {
        assertThrows(IllegalArgumentException.class, () -> new NumberRangeComparer("[1,5"));
    }

    @Test
    void testThrowsExceptionOnMalformedRangeMissingStart() {
        assertThrows(IllegalArgumentException.class, () -> new NumberRangeComparer("1,5]"));
    }

    @Test
    void testThrowsExceptionOnMalformedRangeMissingSeparator() {
        assertThrows(IllegalArgumentException.class, () -> new NumberRangeComparer("[1 5]"));
    }

}
