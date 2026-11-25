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
package com.fortify.cli.aviator.fpr.remediation.applier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DelimiterBalanceTest {
    @Test
    void countsOpeningMinusClosingPerKind() {
        assertArrayEquals(new int[] {1, 0, 0}, DelimiterBalance.of("if (a) {"));
        assertArrayEquals(new int[] {-1, 0, 0}, DelimiterBalance.of("    }"));
        assertArrayEquals(new int[] {0, 1, 1}, DelimiterBalance.of("foo(bar["));
        assertArrayEquals(new int[] {0, 0, 0}, DelimiterBalance.of("} else {"));
    }

    @Test
    void ignoresDelimitersInStringsCharactersAndComments() {
        assertArrayEquals(new int[] {0, 0, 0}, DelimiterBalance.of("String s = \"{ ( [\";"));
        assertArrayEquals(new int[] {0, 0, 0}, DelimiterBalance.of("char c = '{';"));
        assertArrayEquals(new int[] {0, 0, 0}, DelimiterBalance.of("x(); // }}}"));
        assertArrayEquals(new int[] {0, 0, 0}, DelimiterBalance.of("String s = \"a\\\"{\";"));
        assertArrayEquals(new int[] {0, 0, 0}, DelimiterBalance.of(List.of("/* {", " } */")));
        assertArrayEquals(new int[] {1, 0, 0}, DelimiterBalance.of(List.of("/* { */ {")));
    }

    @Test
    void sumsOverSeveralLines() {
        assertArrayEquals(new int[] {0, 0, 0}, DelimiterBalance.of(List.of("try (A a = b()) {", "    use(a);", "}")));
        assertArrayEquals(new int[] {1, 0, 0}, DelimiterBalance.of(List.of("try {", "    run();")));
    }

    @Test
    void recognizesLinesThatAreOnlyDelimiters() {
        assertTrue(DelimiterBalance.isDelimitersOnly("        }"));
        assertTrue(DelimiterBalance.isDelimitersOnly("});"));
        assertTrue(DelimiterBalance.isDelimitersOnly("  ) ]  "));
        assertFalse(DelimiterBalance.isDelimitersOnly("} else {"));
        assertFalse(DelimiterBalance.isDelimitersOnly("run();"));
        assertFalse(DelimiterBalance.isDelimitersOnly("   "));
        assertFalse(DelimiterBalance.isDelimitersOnly(null));
    }

    @Test
    void isBalancedOnlyForAllZero() {
        assertTrue(DelimiterBalance.isBalanced(new int[] {0, 0, 0}));
        assertFalse(DelimiterBalance.isBalanced(new int[] {0, 1, 0}));
    }
}
