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

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.filter.comparer.BooleanComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.ContainsSearchComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.IsNotSearchComparer;

class BooleanComparerTest {

    @Test
    void testOnlyPositiveConditions_OR_Logic() {
        BooleanComparer comparer = new BooleanComparer();
        comparer.addComparer(new ContainsSearchComparer("apple"));
        comparer.addComparer(new ContainsSearchComparer("banana"));

        assertTrue(comparer.matches("apple pie"), "Should match 'apple'");
        assertTrue(comparer.matches("banana bread"), "Should match 'banana'");
        assertFalse(comparer.matches("cherry tart"), "Should not match 'cherry'");
    }

    @Test
    void testOnlyNegativeConditions_AND_Logic() {
        BooleanComparer comparer = new BooleanComparer();
        comparer.addComparer(new IsNotSearchComparer(new ContainsSearchComparer("apple")));
        comparer.addComparer(new IsNotSearchComparer(new ContainsSearchComparer("banana")));

        assertTrue(comparer.matches("cherry tart"), "Should match because it's not apple and not banana");
        assertFalse(comparer.matches("apple pie"), "Should fail because it contains 'apple'");
        assertFalse(comparer.matches("banana bread"), "Should fail because it contains 'banana'");
    }

    @Test
    void testMixedConditions_PositiveAndNegative() {
        BooleanComparer comparer = new BooleanComparer();
        comparer.addComparer(new ContainsSearchComparer("pie")); // OR condition
        comparer.addComparer(new IsNotSearchComparer(new ContainsSearchComparer("apple"))); // AND condition

        assertTrue(comparer.matches("cherry pie"), "Should match: contains 'pie' and not 'apple'");
        assertFalse(comparer.matches("apple pie"), "Should fail: contains 'apple'");
        assertFalse(comparer.matches("banana bread"), "Should fail: does not contain 'pie'");
    }

    @Test
    void testNoMatchWhenANDConditionFails() {
        BooleanComparer comparer = new BooleanComparer();
        comparer.addComparer(new ContainsSearchComparer("pie")); // OR condition
        comparer.addComparer(new IsNotSearchComparer(new ContainsSearchComparer("cherry"))); // AND condition

        assertFalse(comparer.matches("cherry pie"), "Should fail because the AND condition (not cherry) is false");
    }

    @Test
    void testEmptyComparerReturnsFalse() {
        BooleanComparer comparer = new BooleanComparer();
        assertFalse(comparer.matches("anything"));
    }
}
