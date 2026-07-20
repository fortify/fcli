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
package com.fortify.cli.aviator.ssc.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCApplyRemediationsSourceMixin;
import com.fortify.cli.common.exception.FcliSimpleException;

import picocli.CommandLine;

class AviatorSSCApplyRemediationsCommandTest {
    @Test
    void testNormalizeIssueIdsTrimsAndDeduplicates() throws Exception {
        AviatorSSCApplyRemediationsCommand command = parse("--from-cache", "cache.zip", "--issue-ids", " ISSUE-1 , , ISSUE-2,ISSUE-1 ");
        assertEquals(Set.of("ISSUE-1", "ISSUE-2"), AviatorIssueIdFilterUtils.normalizeIssueIds(getIssueIds(command)));
    }

    @Test
    void testNormalizeIssueIdsRejectsOnlyBlankValues() throws Exception {
        AviatorSSCApplyRemediationsCommand command = parse("--from-cache", "cache.zip", "--issue-ids", " ,  , ");
        assertThrows(FcliSimpleException.class, () -> AviatorIssueIdFilterUtils.normalizeIssueIds(getIssueIds(command)));
    }

    @Test
    void testFromCacheParsesPath() throws Exception {
        AviatorSSCApplyRemediationsCommand command = parse("--from-cache", "remediations.zip");
        assertEquals(Path.of("remediations.zip"), getSourceMixin(command).getFromCache());
        assertTrue(getSourceMixin(command).isFromCacheSelected());
    }

    @Test
    void testFromCacheCannotBeCombinedWithArtifactId() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--artifact-id", "1", "--from-cache", "cache.zip"));
    }

    @Test
    void testIssueIdsWithoutFromCacheThrowsException() {
        AviatorSSCApplyRemediationsCommand command = parse("--artifact-id", "1", "--issue-ids", "ISSUE-1");

        assertThrows(FcliSimpleException.class, command::getJsonNode);
    }

    @Test
    void testAggregateMetricsForFilteredAllDeduplicatesAcrossArtifacts() {
        RemediationMetric metricOne = RemediationMetric.filtered(Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-1"), Set.of("A.java"));
        RemediationMetric metricTwo = RemediationMetric.filtered(Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-2"), Set.of("B.java"));

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                Set.of("ISSUE-1", "ISSUE-2"), List.of(metricOne, metricTwo));

        assertEquals(2, aggregated.totalRemediations());
        assertEquals(2, aggregated.appliedRemediations());
        assertEquals(0, aggregated.skippedRemediations());
        assertEquals(Set.of("ISSUE-1", "ISSUE-2"), aggregated.appliedIssueIds());
        assertEquals(Set.of("A.java", "B.java"), aggregated.modifiedFiles());
    }

    @Test
    void testGetRemainingIssueIdsRemovesAlreadyAppliedIds() {
        RemediationMetric metric = RemediationMetric.filtered(Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-1"), Set.of("A.java"));

        Set<String> remainingIssueIds = AviatorRemediationMetricsHelper.getRemainingIssueIds(
                Set.of("ISSUE-1", "ISSUE-2"), metric);

        assertEquals(Set.of("ISSUE-2"), remainingIssueIds);
    }

    private static AviatorSSCApplyRemediationsCommand parse(String... args) {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();
        new CommandLine(command).parseArgs(args);
        return command;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getIssueIds(AviatorSSCApplyRemediationsCommand command) throws Exception {
        Field field = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("issueIds");
        field.setAccessible(true);
        return (List<String>) field.get(command);
    }

    private static AviatorSSCApplyRemediationsSourceMixin getSourceMixin(AviatorSSCApplyRemediationsCommand command) throws Exception {
        Field field = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("sourceSelector");
        field.setAccessible(true);
        return (AviatorSSCApplyRemediationsSourceMixin) field.get(command);
    }
}
