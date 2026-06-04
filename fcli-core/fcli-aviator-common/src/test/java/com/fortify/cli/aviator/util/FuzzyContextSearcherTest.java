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
}