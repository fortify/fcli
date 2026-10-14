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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAuditHelper.AuditableIssuePresence;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;

class AviatorSSCAuditHelperTest {

    @Test
    void excludesPreviouslyProcessedIssuesFromNormalPreflightCount() throws Exception {
        try (var server = new TestSscServer(processedIssue(), legacyAnalysisIssue(), unprocessedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertTrue(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forceReauditPreflightIncludesPreviouslyProcessedIssues() throws Exception {
        try (var server = new TestSscServer(processedIssue(), unprocessedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(2, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertFalse(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forceReauditPreflightIncludesLegacyAnalysisTagCandidates() throws Exception {
        try (var server = new TestSscServer(legacyAnalysisIssue());
                var unirest = newUnirest(server)) {
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
        }
    }

    @Test
    void forceReauditPreflightDoesNotSkipAnAllProcessedApplicationVersion() throws Exception {
        try (var server = new TestSscServer(processedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertFalse(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forceReauditPreflightExcludesAuditedIssuesWithoutAviatorOrLegacyResultTag() throws Exception {
        try (var server = new TestSscServer(processedIssue(), unprocessedIssue(), humanAuditedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(2, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertFalse(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forceReauditPreflightExcludesSuppressedAviatorIssues() throws Exception {
        try (var server = new TestSscServer(processedIssue(), suppressedProcessedIssue(), suppressedLegacyAnalysisIssue());
                var unirest = newUnirest(server)) {
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
        }
    }

    @Test
    void forceReauditCategoryBreakdownIncludesFullyAuditedCategories() throws Exception {
        try (var server = new TestSscServer(processedIssue());
                var unirest = newUnirest(server)) {
            List<Map<String, Object>> categories = AviatorSSCAuditHelper.getTopUnauditedCategories(
                    unirest, appVersion(), logger(), 10, true);

            assertEquals(1, categories.size());
            assertEquals("Category A", categories.get(0).get("categoryName"));
            assertEquals(1L, categories.get(0).get("unauditedCount"));
            assertEquals(1, server.getSelectorSetRequestCount());
        }
    }

    @Test
    void presenceStopsOnFirstPageEvenWhenSscReportsOneMillionIssues() throws Exception {
        var progress = new RecordingProgressWriter();
        try (var server = new TestSscServer(unprocessedIssue()); var unirest = newUnirest(server)) {
            server.reportedCount = 1_000_000L;

            assertEquals(AuditableIssuePresence.PRESENT, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), new AviatorLoggerImpl(progress), true, null, null, false));
            assertEquals(1, server.issueRequestCount);
            assertTrue(server.getLastIssueQuery().contains("q=audited:false"));
            assertTrue(server.getLastIssueQuery().contains("embed=auditValues"));
            assertTrue(server.getLastIssueQuery().contains("showhidden=true"));
            assertEquals("Status: Found auditable issues.", progress.messages.get(progress.messages.size() - 1));
        }
    }

    @Test
    void presenceContinuesPastProcessedAndSuppressedIssuesToFindCandidateOnNextPage() throws Exception {
        var processed = processedIssue().put("audited", false);
        var suppressed = unprocessedIssue().put("suppressed", true);
        ObjectNode[] issues = new ObjectNode[201];
        Arrays.fill(issues, processed);
        issues[199] = suppressed;
        issues[200] = unprocessedIssue();
        try (var server = new TestSscServer(issues); var unirest = newUnirest(server)) {
            assertEquals(AuditableIssuePresence.PRESENT, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(List.of(0, 200), server.issueStarts);
        }
    }

    @Test
    void forcePresenceFindsLegacyCandidateAfterPageOfHumanAuditedIssues() throws Exception {
        ObjectNode[] issues = new ObjectNode[201];
        Arrays.fill(issues, humanAuditedIssue());
        issues[200] = legacyAnalysisIssue();
        try (var server = new TestSscServer(issues); var unirest = newUnirest(server)) {
            assertEquals(AuditableIssuePresence.PRESENT, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertEquals(List.of(0, 200), server.issueStarts);
            assertFalse(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forcePresenceIncludesProcessedIssuesWithoutScanningLaterPages() throws Exception {
        try (var server = new TestSscServer(processedIssue()); var unirest = newUnirest(server)) {
            server.reportedCount = 1_000_000L;
            assertEquals(AuditableIssuePresence.PRESENT, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertEquals(1, server.issueRequestCount);
        }
    }

    @Test
    void forcePresenceExhaustsPagesBeforeReportingNoCandidates() throws Exception {
        ObjectNode[] issues = new ObjectNode[401];
        Arrays.fill(issues, humanAuditedIssue());
        issues[199] = suppressedProcessedIssue();
        // SSC AV 26 issue 27248 is suppressed but unaudited, with no audit tags.
        issues[400] = unprocessedIssue().put("id", 27248).put("suppressed", true).putNull("primaryTag");
        try (var server = new TestSscServer(issues); var unirest = newUnirest(server)) {
            assertEquals(AuditableIssuePresence.NONE, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertEquals(List.of(0, 200, 400), server.issueStarts);
        }
    }

    @Test
    void normalPresenceExcludesProcessedIssuesEvenWhenSscMarksThemUnaudited() throws Exception {
        try (var server = new TestSscServer(processedIssue().put("audited", false)); var unirest = newUnirest(server)) {
            assertEquals(AuditableIssuePresence.NONE, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
        }
    }

    @Test
    void emptyVersionHasNoAuditableIssues() throws Exception {
        try (var server = new TestSscServer(); var unirest = newUnirest(server)) {
            assertEquals(AuditableIssuePresence.NONE, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(1, server.issueRequestCount);
        }
    }

    @Test
    void exactCountStillRetrievesAllPages() throws Exception {
        ObjectNode[] issues = new ObjectNode[401];
        Arrays.fill(issues, unprocessedIssue());
        try (var server = new TestSscServer(issues); var unirest = newUnirest(server)) {
            assertEquals(401, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(List.of(0, 200, 400), server.issueStarts);
        }
    }

    @Test
    void presencePreservesExplicitFilterSetAndFolderWhenNoFilterSetIsAlsoRequested() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue()); var unirest = newUnirest(server)) {
            assertEquals(AuditableIssuePresence.PRESENT, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, "Default", List.of("High"), false));
            assertTrue(server.getLastIssueQuery().contains("filterset=default-guid"));
            assertTrue(server.getLastIssueQuery().contains("filter=FOLDER:high-guid"));
            assertFalse(server.getLastIssueQuery().contains("showhidden=true"));
        }
    }

    @Test
    void missingCountIsUnknownRatherThanAnEmptyResult() throws Exception {
        try (var server = new TestSscServer(); var unirest = newUnirest(server)) {
            server.responseOverride = JsonHelper.getObjectMapper().createObjectNode().set("data",
                    JsonHelper.getObjectMapper().createArrayNode());
            assertEquals(AuditableIssuePresence.UNKNOWN, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(-1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
        }
    }

    @Test
    void prematureEndOfPaginationIsUnknownRatherThanProofOfNoCandidates() throws Exception {
        ObjectNode[] issues = new ObjectNode[200];
        Arrays.fill(issues, humanAuditedIssue());
        try (var server = new TestSscServer(issues); var unirest = newUnirest(server)) {
            server.reportedCount = 201L;
            assertEquals(AuditableIssuePresence.UNKNOWN, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertEquals(List.of(0, 200), server.issueStarts);
            assertEquals(-1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
        }
    }

    @Test
    void invalidIssueDataIsUnknownRatherThanAnEmptyResult() throws Exception {
        try (var server = new TestSscServer(); var unirest = newUnirest(server)) {
            server.responseOverride = JsonHelper.getObjectMapper().createObjectNode().put("count", 0).put("data", "invalid");
            assertEquals(AuditableIssuePresence.UNKNOWN, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
        }
    }

    @Test
    void apiErrorInsideHttpSuccessIsUnknownRatherThanAnEmptyResult() throws Exception {
        try (var server = new TestSscServer(); var unirest = newUnirest(server)) {
            server.responseOverride = JsonHelper.getObjectMapper().createObjectNode().put("count", 0).put("responseCode", 400);
            server.responseOverride.putArray("data");
            assertEquals(AuditableIssuePresence.UNKNOWN, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(-1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
        }
    }

    @Test
    void httpFailurePreservesUnknownCountFallback() throws Exception {
        try (var server = new TestSscServer(); var unirest = newUnirest(server)) {
            server.responseStatus = 400;
            assertEquals(AuditableIssuePresence.UNKNOWN, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(-1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
        }
    }

    @Test
    void serverNormalCountExcludesProcessedUnauditedIssuesAndPreservesVisibilityScope() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), processedIssue().put("audited", false),
                unprocessedIssue().put("hidden", true), unprocessedIssue().put("suppressed", true));
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.respectVisibility = true;

            assertEquals(2, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(3, server.issueRequestCount);
            assertEquals(List.of(0, 0, 0), server.issueStarts);
            assertTrue(server.issueQueries.stream().allMatch(q -> q.contains("limit=1") && q.contains("showhidden=true")));
        }
    }

    @Test
    void serverCountsMatchRecordedSscReferenceForNormalAndForceModes() throws Exception {
        // Uploaded SSC snapshot: 171 unprocessed + 46 processed with Analysis set.
        ObjectNode[] issues = new ObjectNode[217];
        Arrays.fill(issues, 0, 171, unprocessedIssue());
        Arrays.fill(issues, 171, 217, processedWithAnalysisIssue());
        try (var server = new TestSscServer(issues); var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            assertEquals(171, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), false, null, null, false));
            assertEquals(217, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), false, null, null, true));
            assertEquals(5, server.issueRequestCount);
            assertTrue(server.issueQueries.stream().allMatch(q -> q.contains("filterset=default-guid")));
        }
    }

    @Test
    void serverNormalCountUsesFixedRequestsForOneMillionCandidates() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), processedIssue().put("audited", false));
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.countByFilter.put("", 2_000_000L);
            server.countByFilter.put(statusFilter(""), 1_000_000L);
            server.countByFilter.put(statusFilter("0"), 1_000_000L);

            assertEquals(1_000_000L, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(3, server.issueRequestCount);
            assertTrue(server.issueQueries.stream().allMatch(q -> q.contains("limit=1")));
            assertFalse(server.issueQueries.stream().anyMatch(q -> q.contains("(") || q.contains(")")));
        }
    }

    @Test
    void presenceProvesNoCandidatesWithoutScanningOneMillionProcessedIssues() throws Exception {
        try (var server = new TestSscServer(processedIssue().put("audited", false)); var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.countByFilter.put("", 1_000_000L);
            server.countByFilter.put(statusFilter("0"), 1_000_000L);
            assertEquals(AuditableIssuePresence.NONE, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(4, server.issueRequestCount);
            assertTrue(server.issueStarts.stream().allMatch(start -> start == 0));
        }
    }

    @Test
    void presenceFindsSparseCandidateUsingVerifiedServerCount() throws Exception {
        try (var server = new TestSscServer(processedIssue().put("audited", false), unprocessedIssue());
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.pageSizeCap = 1;
            server.countByFilter.put("", 1_000_000L);
            server.countByFilter.put(statusFilter(""), 1L);
            server.countByFilter.put(statusFilter("0"), 999_999L);
            assertEquals(AuditableIssuePresence.PRESENT, AviatorSSCAuditHelper.getAuditableIssuePresence(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertEquals(4, server.issueRequestCount);
            assertTrue(server.issueStarts.stream().allMatch(start -> start == 0));
        }
    }

    @Test
    void ignoredSelectorFallsBackToExactPagination() throws Exception {
        ObjectNode[] issues = new ObjectNode[401];
        Arrays.fill(issues, processedIssue().put("audited", false));
        issues[0] = unprocessedIssue();
        try (var server = new TestSscServer(issues); var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.ignoreTagFilters = true;

            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertTrue(server.issueQueries.stream().anyMatch(q -> q.contains("limit=200") && q.contains("start=400")));
        }
    }

    @Test
    void nonPartitioningSelectorCountsFallBackToExactPagination() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), processedIssue().put("audited", false));
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.countByFilter.put(statusFilter("0"), 2L);

            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertTrue(server.getLastIssueQuery().contains("limit=200"));
        }
    }

    @Test
    void suppressedCountPreviewFallsBackToClientEligibilityCheck() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue().put("suppressed", true), unprocessedIssue());
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.ignoreSuppressionFlag = true;
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertTrue(server.getLastIssueQuery().contains("limit=200"));
        }
    }

    @Test
    void normalServerCountExcludesSuppressedIssuesAfterPreview() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), unprocessedIssue().put("suppressed", true));
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            long optimized = AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false);
            assertEquals(1, optimized);
            assertEquals(3, server.issueRequestCount);
            assertTrue(server.issueQueries.stream().allMatch(q -> q.contains("showsuppressed=false")));

            server.tagSelectorsAvailable = false;
            assertEquals(optimized, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
        }
    }

    @Test
    void forceServerCountExcludesSuppressedIssuesAfterPreview() throws Exception {
        // Suppression does not imply audited=true; this state was captured from SSC AV 26.
        var suppressed = unprocessedIssue().put("id", 27248).put("suppressed", true).putNull("primaryTag");
        try (var server = new TestSscServer(unprocessedIssue(), suppressed);
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            long optimized = AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true);
            assertEquals(1, optimized);
            assertEquals(2, server.issueRequestCount);
            assertTrue(server.issueQueries.stream().allMatch(q -> q.contains("showsuppressed=false")));

            server.tagSelectorsAvailable = false;
            assertEquals(optimized, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
        }
    }

    @Test
    void unsupportedSuppressionFlagFallsBackToClientEligibilityCheck() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), suppressedLegacyAnalysisIssue());
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.rejectSuppressionFlag = true;
            for (boolean force : List.of(false, true)) {
                assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                        unirest, appVersion(), logger(), true, null, null, force));
                assertTrue(server.getLastIssueQuery().contains("limit=200"));
                assertFalse(server.getLastIssueQuery().contains("showsuppressed"));
            }
        }
    }

    @Test
    void filteredCountErrorDoesNotSkipAnOtherwiseAuditableVersion() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue()); var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.rejectTagFilters = true;
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertTrue(server.getLastIssueQuery().contains("limit=200"));
        }
    }

    @Test
    void forceServerCountIncludesProcessedAndLegacyIssuesWithoutDoubleCounting() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), processedWithAnalysisIssue(), legacyAnalysisIssue());
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            assertEquals(3, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertEquals(2, server.issueRequestCount);
            assertTrue(server.getLastIssueQuery().contains("q=audited:true"));
            assertFalse(server.issueQueries.stream().anyMatch(q -> q.contains("(") || q.contains(")")));
        }
    }

    @Test
    void forceServerCountFallsBackWhenAuditedIssuesLackAnalysis() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), processedIssue(), humanAuditedIssue(), legacyAnalysisIssue());
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            assertEquals(3, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertEquals(3, server.issueRequestCount);
            assertTrue(server.getLastIssueQuery().contains("limit=200"));
        }
    }

    @Test
    void forceServerCountUsesFixedRequestsForOneMillionCandidates() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), processedWithAnalysisIssue());
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.countByFilter.put("", 1_000_000L);
            assertEquals(1_000_000L, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertEquals(2, server.issueRequestCount);
        }
    }

    @Test
    void forceIgnoredAnalysisFilterFallsBackToClientEligibilityCheck() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue(), legacyAnalysisIssue(), humanAuditedIssue());
                var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            server.ignoreTagFilters = true;
            assertEquals(2, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertTrue(server.getLastIssueQuery().contains("limit=200"));
        }
    }

    @Test
    void folderCountsRetainExistingPaginationAndQuerySyntax() throws Exception {
        try (var server = new TestSscServer(unprocessedIssue()); var unirest = newUnirest(server)) {
            server.tagSelectorsAvailable = true;
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), false, "Default", List.of("High"), false));
            assertEquals(1, server.issueRequestCount);
            assertEquals(0, server.selectorSetRequestCount);
            assertTrue(server.getLastIssueQuery().contains("filter=FOLDER:high-guid"));
            assertTrue(server.getLastIssueQuery().contains("filterset=default-guid"));
        }
    }

    private static String statusFilter(String index) {
        return "CUSTOMTAG[" + AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid() + "]:" + index;
    }

    private static SSCAppVersionDescriptor appVersion() {
        var descriptor = new SSCAppVersionDescriptor();
        descriptor.setVersionId("42");
        descriptor.setApplicationName("TestApp");
        descriptor.setVersionName("1.0");
        return descriptor;
    }

    private static AviatorLoggerImpl logger() {
        return new AviatorLoggerImpl(new RecordingProgressWriter());
    }

    private static UnirestInstance newUnirest(TestSscServer server) {
        return UnirestHelper.createUnirestInstance(unirest -> {
            UnirestJsonHeaderConfigurer.configure(unirest);
            unirest.config().defaultBaseUrl(server.getBaseUrl());
        });
    }

    private static ObjectNode processedIssue() {
        ObjectNode issue = JsonHelper.getObjectMapper().createObjectNode();
        issue.put("audited", true);
        ArrayNode auditValues = issue.putObject("_embed").putArray("auditValues");
        auditValues.addObject()
                .put("customTagGuid", AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid())
                .put("customTagIndex", 0);
        return issue;
    }

    private static ObjectNode unprocessedIssue() {
        ObjectNode issue = JsonHelper.getObjectMapper().createObjectNode();
        issue.put("audited", false);
        issue.putObject("_embed").putArray("auditValues");
        return issue;
    }

    private static ObjectNode legacyAnalysisIssue() {
        ObjectNode issue = JsonHelper.getObjectMapper().createObjectNode();
        issue.put("audited", true);
        issue.putObject("_embed").putArray("auditValues")
                .addObject()
                .put("customTagGuid", Constants.ANALYSIS_TAG_ID)
                .put("customTagIndex", 4);
        return issue;
    }

    private static ObjectNode processedWithAnalysisIssue() {
        ObjectNode issue = processedIssue();
        ((ArrayNode) issue.path("_embed").path("auditValues")).addObject()
                .put("customTagGuid", Constants.ANALYSIS_TAG_ID).put("customTagIndex", 4);
        return issue;
    }

    private static ObjectNode humanAuditedIssue() {
        ObjectNode issue = JsonHelper.getObjectMapper().createObjectNode();
        issue.put("audited", true);
        issue.putObject("_embed").putArray("auditValues");
        return issue;
    }

    private static ObjectNode suppressedProcessedIssue() {
        ObjectNode issue = processedIssue();
        issue.put("suppressed", true);
        return issue;
    }

    private static ObjectNode suppressedLegacyAnalysisIssue() {
        ObjectNode issue = legacyAnalysisIssue();
        issue.put("suppressed", true);
        return issue;
    }

    private static final class TestSscServer implements AutoCloseable {
        private final HttpServer server;
        private final ArrayNode issues = JsonHelper.getObjectMapper().createArrayNode();
        private int selectorSetRequestCount;
        private int issueRequestCount;
        private final List<Integer> issueStarts = new ArrayList<>();
        private final List<String> issueQueries = new ArrayList<>();
        private final Map<String, Long> countByFilter = new HashMap<>();
        private boolean tagSelectorsAvailable;
        private boolean ignoreTagFilters;
        private boolean rejectTagFilters;
        private boolean ignoreSuppressionFlag;
        private boolean rejectSuppressionFlag;
        private boolean respectVisibility;
        private int pageSizeCap = 200;
        private Long reportedCount;
        private ObjectNode responseOverride;
        private int responseStatus = 200;
        private String lastIssueQuery = "";

        private TestSscServer(ObjectNode... issueNodes) throws IOException {
            for (ObjectNode issue : issueNodes) {
                issues.add(issue);
            }
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/projectVersions/42/issues", this::handleIssues);
            server.createContext("/api/v1/projectVersions/42/issueSelectorSet", this::handleSelectorSet);
            server.createContext("/api/v1/projectVersions/42/issueGroups", this::handleIssueGroups);
            server.createContext("/api/v1/projectVersions/42/filterSets", this::handleFilterSets);
            server.start();
        }

        private String getBaseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        private void handleIssues(HttpExchange exchange) throws IOException {
            issueRequestCount++;
            lastIssueQuery = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            issueQueries.add(lastIssueQuery);
            Map<String, String> query = new HashMap<>();
            for (String parameter : exchange.getRequestURI().getRawQuery().split("&")) {
                String[] pair = parameter.split("=", 2);
                query.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
            int start = Integer.parseInt(query.getOrDefault("start", "0"));
            int limit = Math.min(pageSizeCap, Integer.parseInt(query.getOrDefault("limit", "200")));
            issueStarts.add(start);
            if (responseOverride != null || responseStatus != 200) {
                writeJson(exchange, responseOverride != null ? responseOverride
                        : JsonHelper.getObjectMapper().createObjectNode());
                return;
            }
            ArrayNode matchingIssues = JsonHelper.getObjectMapper().createArrayNode();
            String filter = query.get("filter");
            if (rejectTagFilters && filter != null && filter.startsWith("CUSTOMTAG")
                    || rejectSuppressionFlag && query.containsKey("showsuppressed")) {
                byte[] error = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, error.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(error);
                }
                return;
            }
            for (JsonNode issue : issues) {
                if ("audited:false".equals(query.get("q")) && issue.path("audited").asBoolean(false)
                        || "audited:true".equals(query.get("q")) && !issue.path("audited").asBoolean(false)) {
                    continue;
                }
                boolean excludeSuppressed = !ignoreSuppressionFlag && "false".equals(query.get("showsuppressed"))
                        || respectVisibility && !"true".equals(query.get("showsuppressed"));
                if (excludeSuppressed && issue.path("suppressed").asBoolean(false)
                        || respectVisibility && issue.path("hidden").asBoolean(false) && !"true".equals(query.get("showhidden"))) {
                    continue;
                }
                if (!ignoreTagFilters && filter != null && filter.startsWith("CUSTOMTAG") && !matchesTagFilter(issue, filter)) {
                    continue;
                }
                matchingIssues.add(issue);
            }
            ArrayNode page = JsonHelper.getObjectMapper().createArrayNode();
            for (int index = start; index < Math.min(start + limit, matchingIssues.size()); index++) {
                page.add(matchingIssues.get(index));
            }
            long count = countByFilter.getOrDefault(filter != null ? filter : "",
                    reportedCount != null ? reportedCount : (long) matchingIssues.size());
            writeJson(exchange, JsonHelper.getObjectMapper().createObjectNode().put("count", count).set("data", page));
        }

        private boolean matchesTagFilter(JsonNode issue, String filter) {
            String guid = filter.substring(filter.indexOf('[') + 1, filter.indexOf(']'));
            String index = filter.substring(filter.lastIndexOf(':') + 1);
            var matchingTags = JsonHelper.stream((ArrayNode) issue.path("_embed").path("auditValues"))
                    .filter(tag -> guid.equals(tag.path("customTagGuid").asText())).toList();
            return index.isEmpty() ? matchingTags.isEmpty()
                    : matchingTags.stream().anyMatch(tag -> index.equals(tag.path("customTagIndex").asText()));
        }

        private void handleFilterSets(HttpExchange exchange) throws IOException {
            ObjectNode filterSet = JsonHelper.getObjectMapper().createObjectNode()
                    .put("guid", "default-guid").put("title", "Default").put("defaultFilterSet", true);
            filterSet.putArray("folders").addObject().put("guid", "high-guid").put("name", "High");
            writeJson(exchange, JsonHelper.getObjectMapper().createObjectNode().set("data",
                    JsonHelper.getObjectMapper().createArrayNode().add(filterSet)));
        }

        private void handleSelectorSet(HttpExchange exchange) throws IOException {
            selectorSetRequestCount++;
            ObjectNode data = JsonHelper.getObjectMapper().createObjectNode();
            ArrayNode filters = data.putArray("filterBySet");
            if (tagSelectorsAvailable) {
                addTagSelector(filters, AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status");
                addTagSelector(filters, Constants.ANALYSIS_TAG_ID, "Analysis");
            }
            data.putArray("groupBySet").addObject()
                    .put("guid", "category-guid")
                    .put("displayName", "Category");
            writeJson(exchange, JsonHelper.getObjectMapper().createObjectNode().set("data", data));
        }

        private void addTagSelector(ArrayNode filters, String guid, String name) {
            ObjectNode selector = filters.addObject().put("entityType", "CUSTOMTAG").put("guid", guid)
                    .put("value", guid).put("displayName", name).put("filterSelectorType", "LIST");
            ArrayNode options = selector.putArray("selectorOptions");
            options.addObject().put("value", "").put("displayName", "Not Set");
            options.addObject().put("value", "0").put("displayName", "Value 0");
        }

        private void handleIssueGroups(HttpExchange exchange) throws IOException {
            ObjectNode group = JsonHelper.getObjectMapper().createObjectNode()
                    .put("id", "Category A")
                    .put("visibleCount", 1)
                    .put("auditedCount", 1);
            ArrayNode data = JsonHelper.getObjectMapper().createArrayNode().add(group);
            writeJson(exchange, JsonHelper.getObjectMapper().createObjectNode().set("data", data));
        }

        private void writeJson(HttpExchange exchange, ObjectNode responseNode) throws IOException {
            byte[] response = responseNode.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, response.length);
            try (var outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        }

        private String getLastIssueQuery() {
            return lastIssueQuery;
        }

        private int getSelectorSetRequestCount() {
            return selectorSetRequestCount;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class RecordingProgressWriter implements IProgressWriter {
        private final List<String> messages = new ArrayList<>();
        @Override public boolean isMultiLineSupported() { return false; }
        @Override public void writeProgress(String message, Object... args) { messages.add(String.format(message, args)); }
        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) {}
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void clearProgress() {}
        @Override public void close() {}
        @Override public String type() { return "test"; }
    }
}
