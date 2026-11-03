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
import java.util.stream.Stream;

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
    void issueChangesAreSummarizedByStrongestAttributions() {
        var issues = issues(List.of(issue("new1")), List.of(),
                List.of(issue("removed1"), issue("removed2"), issue("other")));
        var changes = changes(null, List.of(new FoDScanCompareChangeSet("URL", List.of("removed1", "removed2", "x"))));
        var attribution = attribute(issues, changes, List.of(POLICY_NARROWED));
        assertEquals("1 new issue (1 indeterminate); "
                + "3 fixed issues (strongest attributions: URL_REMOVED (2) and SCAN_POLICY_CHANGE (1)).",
                new FoDScanCompareSummary(issues, List.of("policyName"), changes, attribution).getText());
    }

    @Test
    void singleCauseExplainingAllIssuesOmitsCount() {
        var issues = issues(List.of(), List.of(), List.of(issue("removed1"), issue("removed2")));
        var changes = changes(null, List.of());
        assertEquals("2 fixed issues (strongest attribution: URL_REMOVED).",
                new FoDScanCompareSummary(issues, List.of(), changes, attribute(issues, changes, List.of())).getText());
    }

    @Test
    void eachTransitionHasItsOwnAttributionForSastAndDast() {
        for ( var prefix : List.of("FILE", "URL") ) {
            var issues = issues(List.of(issue(prefix + "_ADDED")), List.of(issue(prefix + "_ADDED")),
                    List.of(issue(prefix + "_REMOVED")));
            var changes = changes(null, List.of());
            assertEquals("1 new issue (strongest attribution: " + prefix + "_ADDED); "
                    + "1 reopened issue (strongest attribution: " + prefix + "_ADDED); "
                    + "1 fixed issue (strongest attribution: " + prefix + "_REMOVED).",
                    new FoDScanCompareSummary(issues, List.of(), changes, attributeNamedCauses(issues, changes)).getText());
        }
    }

    @Test
    void boundedCausesUseStableOrderAndOnlyStrongestBasisWithIndeterminateCount() {
        var issues = issues(List.of(issue("FILE_ADDED")), List.of(issue("FILE_ADDED")), List.of(
                issue("FILE_REMOVED FILE_CHANGED FILE_MOVED SOURCE_FILE_REMOVED SOURCE_FILE_CHANGED"),
                issue("FILE_REMOVED FILE_CHANGED"), issue("FILE_REMOVED"), issue("FILE_MOVED"),
                issue("SOURCE_FILE_CHANGED"), issue("FILE_MATCH_AMBIGUOUS")));
        var changes = changes("Manifest not available for scan 1", List.of());
        var attribution = attributeNamedCauses(issues, changes);
        var expected = "1 new issue (strongest attribution: FILE_ADDED); "
                + "1 reopened issue (strongest attribution: FILE_ADDED); "
                + "6 fixed issues (strongest attributions: FILE_REMOVED (3), FILE_CHANGED (2), "
                + "FILE_MOVED (2) and 2 other causes; 1 indeterminate). Attribution counts may overlap. "
                + "Manifest not available for scan 1, so scan changes may be incomplete.";
        var summary = new FoDScanCompareSummary(issues, List.of(), changes, attribution);
        assertEquals(expected, summary.getText());
        issues.addTo(JsonHelper.getObjectMapper().createObjectNode(), 1);
        assertEquals(expected, summary.getText());
    }

    @Test
    void overlappingDastCausesIncludeCountsEvenWhenEachCoversAllIssues() {
        var issues = issues(List.of(), List.of(),
                List.of(issue("URL_REMOVED URL_METHOD_CHANGED URL_STATUS_CHANGED TARGET_CHANGE")));
        var changes = changes(null, List.of());
        assertEquals("1 fixed issue (strongest attributions: URL_REMOVED (1), URL_METHOD_CHANGED (1), "
                + "URL_STATUS_CHANGED (1) and 1 other cause). Attribution counts may overlap.",
                new FoDScanCompareSummary(issues, List.of(), changes, attributeNamedCauses(issues, changes)).getText());
    }

    @Test
    void indeterminateIssuesIncludeAbsentAndPossibleOnlyAttributions() {
        var issues = issues(List.of(issue("none"), issue("FILE_MATCH_AMBIGUOUS")), List.of(), List.of());
        var changes = changes(null, List.of());
        assertEquals("2 new issues (2 indeterminate).",
                new FoDScanCompareSummary(issues, List.of(), changes, attributeNamedCauses(issues, changes)).getText());
    }

    @Test
    void singlePartialCauseIncludesItsCountAndIndeterminateCount() {
        var issues = issues(List.of(), List.of(), List.of(issue("removed1"), issue("other")));
        var changes = changes(null, List.of());
        assertEquals("2 fixed issues (strongest attribution: URL_REMOVED (1); 1 indeterminate).",
                new FoDScanCompareSummary(issues, List.of(), changes, attribute(issues, changes, List.of())).getText());
    }

    @Test
    void noIssueOrScanChangesWithAttribution() {
        var issues = issues(List.of(), List.of(), List.of());
        var changes = changes(null, List.of());
        assertEquals("No issue or scan changes detected.",
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
        return attribute(issues, changes, conditions, URL_REMOVED);
    }

    private static FoDAttributionResult attributeNamedCauses(FoDScanCompareIssues issues, IFoDScanCompareChanges changes) {
        return attribute(issues, changes, List.of(), (context, issue) ->
                Stream.of(issue.node().path("primaryLocationFull").asText().split(" "))
                        .filter(name -> !name.equals("none"))
                        .map(name -> new FoDIssueEvidence(FoDAttributionCause.valueOf(name), name)).toList());
    }

    private static FoDAttributionResult attribute(FoDScanCompareIssues issues, IFoDScanCompareChanges changes,
            List<FoDScanCondition> conditions, IFoDIssueEvidenceMatcher matcher) {
        var context = FoDAttributionContext.builder().changeThresholdPercent(10).metrics(Map.of())
                .changeIndex(changes.getChangeIndex()).issues(issues.toAttributionIssues()).build();
        IFoDScanConditionDetector detector = c -> conditions;
        return new FoDAttributionEngine(List.of(detector), List.of(matcher)).attribute(context);
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
