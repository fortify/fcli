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
package com.fortify.cli.aviator._common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;

class AviatorIssueIdFilterUtilsTest {

    @Test
    void normalizeTrimsAndDeduplicates() {
        assertEquals(
                Set.of("ISSUE-1", "ISSUE-2"),
                AviatorIssueIdFilterUtils.normalizeIssueIds(List.of(" ISSUE-1 ", "", "ISSUE-2", "ISSUE-1")));
    }

    @Test
    void normalizeRejectsOnlyBlankValues() {
        assertThrows(FcliSimpleException.class,
                () -> AviatorIssueIdFilterUtils.normalizeIssueIds(List.of(" ", "", "  ")));
    }
}
