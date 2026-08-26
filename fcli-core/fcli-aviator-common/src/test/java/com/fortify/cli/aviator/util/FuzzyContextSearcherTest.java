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
package com.fortify.cli.aviator.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class FuzzyContextSearcherTest {

    @Test
    void shouldReturnNotFoundWhenOriginalCodeRunsPastEndOfSource() {
        int[] lineFromTo = FuzzyContextSearcher.fuzzySearchOriginalCode(
                List.of("line one", "line two"),
                List.of("line two", "line three"),
                0,
                1);

        assertArrayEquals(new int[] {-1, -1}, lineFromTo);
    }

    @Test
    void shouldMatchOriginalCodeAcrossBlankSourceLines() {
        int[] lineFromTo = FuzzyContextSearcher.fuzzySearchOriginalCode(
                List.of("line one", "", "", "line two"),
                List.of("line one", "line two"),
                0,
                0);

        assertArrayEquals(new int[] {0, 3}, lineFromTo);
    }

    @Test
    void shouldReturnAllMatchingContextStartLines() throws Exception {
        List<Integer> matches = FuzzyContextSearcher.fuzzySearchContextMatches(
                List.of("before", "target", "after", "target", "after"),
                List.of("target", "after"),
                0);

        assertEquals(List.of(1, 3), matches);
    }

    @Test
    void shouldReturnOneMatchingContextStartLine() throws Exception {
        List<Integer> matches = FuzzyContextSearcher.fuzzySearchContextMatches(
                List.of("before", "target", "after"),
                List.of("target", "after"),
                0);

        assertEquals(List.of(1), matches);
    }

    @Test
    void shouldReturnNoMatchingContextStartLines() throws Exception {
        List<Integer> matches = FuzzyContextSearcher.fuzzySearchContextMatches(
                List.of("before", "after"),
                List.of("target", "after"),
                0);

        assertEquals(List.of(), matches);
    }

    @Test
    void shouldKeepPhysicalStartWhenContextBeginsWithBlankLine() throws Exception {
        List<Integer> matches = FuzzyContextSearcher.fuzzySearchContextMatches(
                List.of("header", "", "target", "after"),
                List.of("", "target", "after"),
                0);

        assertEquals(List.of(1), matches);
    }
}