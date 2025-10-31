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
package com.fortify.cli.fod.issue.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fortify.cli.fod.issue.helper.FoDIssueHelper.IssueAggregationData;

/**
 * Minimal tests for fast-output option logic. These are unit-level without invoking full CLI parsing.
 * NOTE: Real integration tests would mock Unirest and command invocation; here we just validate helper behavior.
 */
public class FoDIssueListCommandFastOutputTest {
    @Test
    void testBlankAggregationFactory() {
        IssueAggregationData blank = IssueAggregationData.blank();
        assertEquals("N/A", blank.getIdsString());
        assertEquals(0, blank.getIds().size());
        assertEquals("N/A", blank.getReleaseNamesString());
    }
}