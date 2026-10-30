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
package com.fortify.cli.fod._common.scan.helper.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionCause;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionContext;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionDirection;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionEngine;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionMetric;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionResult;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDIssueEvidence;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDScanCondition;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDAttributionChangeIndex;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDIssueEvidenceMatcher;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDScanConditionDetector;

class FoDScanCompareSummaryTest {
    private static final FoDScanCondition POLICY_NARROWED = new FoDScanCondition(
            FoDAttributionCause.SCAN_POLICY_CHANGE, FoDAttributionDirection.DECREASE, List.of("policyName: Standard -> Passive"));
    // Directly attributes issues at locations starting with 'removed' to URL_REMOVED
    private static final IFoDIssueEvidenceMatcher URL_REMOVED = (context, issue) -> {
        var location = issue.node().path("primaryLocationFull").asText();
        if ( !location.startsWith("removed") ) { return List.of(); }
        context.referenceChange(location);
        return List.of(new FoDIssueEvidence(FoDAttributionCause.URL_REMOVED, "removed"));
    };

    @Test
    void issueChangesAreSummarizedByDrivingCauses() {
        var issues = issues(List.of(issue("new1")), List.of(),
                List.of(issue("removed1"), issue("removed2"), issue("other")));
        var changes = changes(null, List.of(new FoDScanCompareChangeSet("URL", List.of("removed1", "removed2", "x"))));
        var attribution = attribute(issues, changes, List.of(POLICY_NARROWED));
        assertEquals("1 new issue (indeterminate); 3 fixed issues driven by URL_REMOVED (2) and SCAN_POLICY_CHANGE (1).",
                new FoDScanCompareSummary(issues, List.of("policyName"), changes, attribution).getText());
    }

    @Test
    void singleCauseExplainingAllIssuesOmitsCount() {
        var issues = issues(List.of(), List.of(), List.of(issue("removed1"), issue("removed2")));
        var changes = changes(null, List.of());
        assertEquals("2 fixed issues driven by URL_REMOVED.",
                new FoDScanCompareSummary(issues, List.of(), changes, attribute(issues, changes, List.of())).getText());
    }

    @Test
    void scanChangesWithoutIssueChangesAreReported() {
        var issues = issues(List.of(), List.of(), List.of());
        var changes = changes(null, List.of(
                new FoDScanCompareChangeSet("file", List.of("a.java")),
                new FoDScanCompareChangeSet("file", List.of()),
                new FoDScanCompareChangeSet("file", List.of("b.java", "c.java"))));
        var summary = new FoDScanCompareSummary(issues, List.of("scanToolVersion"), changes,
                attribute(issues, changes, List.of(POLICY_NARROWED))).getText();
        assertEquals("No new, reopened or fixed issues, despite changed setting (scanToolVersion), "
                + "scan conditions (SCAN_POLICY_CHANGE) and 3 file changes.", summary);
    }

    @Test
    void noChangesAreReportedWithSkipReason() {
        var issues = issues(List.of(), List.of(), List.of());
        var changes = changes("Manifest not available for scan 1", List.of());
        assertEquals("No issue or scan changes detected. Manifest not available for scan 1, so scan changes may be incomplete.",
                new FoDScanCompareSummary(issues, List.of(), changes, null).getText());
    }

    @Test
    void issueChangesWithoutAttribution() {
        var issues = issues(List.of(), List.of(issue("a"), issue("b")), List.of());
        assertEquals("2 reopened issues.", new FoDScanCompareSummary(issues, List.of(), changes(null, List.of()), null).getText());
    }

    private static ObjectNode issue(String location) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("vulnId", location).put("primaryLocationFull", location).put("category", "XSS").put("severity", 3);
    }

    private static FoDScanCompareIssues issues(List<ObjectNode> newIssues, List<ObjectNode> reopened, List<ObjectNode> fixed) {
        return new FoDScanCompareIssues(newIssues, reopened, fixed);
    }

    private static FoDAttributionResult attribute(FoDScanCompareIssues issues, IFoDScanCompareChanges changes,
            List<FoDScanCondition> conditions) {
        var context = FoDAttributionContext.builder().changeThresholdPercent(10).metrics(Map.of())
                .changeIndex(changes.getChangeIndex()).issues(issues.toAttributionIssues()).build();
        IFoDScanConditionDetector detector = c -> conditions;
        return new FoDAttributionEngine(List.of(detector), List.of(URL_REMOVED)).attribute(context);
    }

    private static IFoDScanCompareChanges changes(String skipReason, List<FoDScanCompareChangeSet> changeSets) {
        return new IFoDScanCompareChanges() {
            @Override public void addTo(ObjectNode result, Predicate<String> includeChange) {}
            @Override public IFoDAttributionChangeIndex getChangeIndex() { return null; }
            @Override public String getSkipReason() { return skipReason; }
            @Override public List<FoDScanCompareChangeSet> getChangeSets() { return changeSets; }
            @Override public Map<String, FoDAttributionMetric> getMetrics() { return Map.of(); }
        };
    }
}
