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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

class FoDAttributionEngineTest {
    @Test
    void scanConditionsOnlyExplainDeclaredTransitions() {
        var analyzer = condition(FoDAttributionCause.ANALYZER_CHANGE, FoDAttributionDirection.ANY);
        var newIssue = issue("A.java", "SQL Injection", FoDAttributionTransition.NEW);
        var reopened = issue("B.java", "SQL Injection", FoDAttributionTransition.REOPENED);
        attribute(List.of(analyzer), List.of(), newIssue, reopened);
        assertStatus(newIssue, "INFERRED");
        assertEquals("SCAN", attributions(newIssue).get(0).get("scope").asText());
        assertStatus(reopened, "UNATTRIBUTED");
    }

    @Test
    void directionalConditionsDependOnDirection() {
        var narrowed = condition(FoDAttributionCause.SCAN_POLICY_CHANGE, FoDAttributionDirection.DECREASE);
        var fixed = issue("A.java", "XSS", FoDAttributionTransition.FIXED);
        var newIssue = issue("B.java", "XSS", FoDAttributionTransition.NEW);
        attribute(List.of(narrowed), List.of(), fixed, newIssue);
        assertStatus(fixed, "INFERRED");
        assertStatus(newIssue, "UNATTRIBUTED");
    }

    @Test
    void recoveryExplainsReopenedButOnlyPossiblyNewIssues() {
        var recovered = condition(FoDAttributionCause.COVERAGE_CHANGE, FoDAttributionDirection.INCREASE);
        var reopened = issue("/a", "XSS", FoDAttributionTransition.REOPENED);
        var newIssue = issue("/b", "XSS", FoDAttributionTransition.NEW);
        attribute(List.of(recovered), List.of(), reopened, newIssue);
        assertStatus(reopened, "INFERRED");
        assertStatus(newIssue, "INDETERMINATE");
    }

    @Test
    void multipleAttributionsAreRecordedStrongestFirst() {
        var analyzer = condition(FoDAttributionCause.ANALYZER_CHANGE, FoDAttributionDirection.ANY);
        IFoDIssueEvidenceMatcher fileChanged = (c, i) -> List.of(new FoDIssueEvidence(FoDAttributionCause.FILE_CHANGED, "changed"));
        var newIssue = issue("A.java", "XSS", FoDAttributionTransition.NEW);
        var result = attribute(List.of(analyzer), List.of(fileChanged), newIssue);
        assertStatus(newIssue, "DIRECT");
        var attributions = attributions(newIssue);
        assertEquals(2, attributions.size());
        assertEquals("FILE_CHANGED", attributions.get(0).get("cause").asText());
        assertEquals("changed", attributions.get(0).get("evidence").asText());
        assertEquals("ANALYZER_CHANGE", attributions.get(1).get("cause").asText());
        var summary = JsonHelper.getObjectMapper().createObjectNode();
        result.addTo(summary);
        assertEquals(1, summary.at("/attributionSummary/new/statuses/DIRECT").asInt());
        assertEquals(1, summary.at("/attributionSummary/new/causes/ANALYZER_CHANGE").asInt());
    }

    @Test
    void issueEvidenceIrrelevantForTransitionIsIgnored() {
        IFoDIssueEvidenceMatcher fileRemoved = (c, i) -> List.of(new FoDIssueEvidence(FoDAttributionCause.FILE_REMOVED, "removed"));
        var newIssue = issue("A.java", "XSS", FoDAttributionTransition.NEW);
        attribute(List.of(), List.of(fileRemoved), newIssue);
        assertStatus(newIssue, "UNATTRIBUTED");
    }

    @Test
    void scanLevelConditionsAreCappedAtInferred() {
        var target = condition(FoDAttributionCause.TARGET_CHANGE, FoDAttributionDirection.ANY);
        var fixed = issue("http://old/a", "XSS", FoDAttributionTransition.FIXED);
        attribute(List.of(target), List.of(), fixed);
        assertStatus(fixed, "INFERRED");
    }

    @Test
    void pairedAtUnchangedLocationForcesIndeterminate() {
        var analyzer = condition(FoDAttributionCause.ANALYZER_CHANGE, FoDAttributionDirection.ANY);
        var fixed = issue("Index.cshtml", "XSS", FoDAttributionTransition.FIXED);
        var newIssue = issue("Index.cshtml", "XSS", FoDAttributionTransition.NEW);
        var other = issue("Other.cshtml", "XSS", FoDAttributionTransition.NEW);
        attribute(List.of(analyzer), List.of(new FoDAttributionPairedLocationMatcher()), fixed, newIssue, other);
        assertStatus(fixed, "INDETERMINATE");
        assertStatus(newIssue, "INDETERMINATE");
        assertEquals(2, attributions(fixed).size());
        assertStatus(other, "INFERRED");
    }

    @Test
    void pairedAtChangedLocationIsNotIndeterminate() {
        var index = new FoDAttributionFileChangeIndex(Map.of("/Src/Index.cshtml", "100"), Map.of("/Src/Index.cshtml", "120"));
        var fixed = issue("Index.cshtml", "XSS", FoDAttributionTransition.FIXED);
        var newIssue = issue("Index.cshtml", "XSS", FoDAttributionTransition.NEW);
        var context = FoDAttributionContext.builder().changeThresholdPercent(10).changeIndex(index)
                .issues(List.of(fixed, newIssue)).build();
        new FoDAttributionEngine(List.of(),
                List.of(new FoDAttributionFileChangeMatcher(), new FoDAttributionPairedLocationMatcher())).attribute(context);
        assertStatus(fixed, "DIRECT");
        assertStatus(newIssue, "DIRECT");
    }

    static FoDAttributionIssue issue(String location, String category, FoDAttributionTransition transition) {
        ObjectNode node = JsonHelper.getObjectMapper().createObjectNode()
                .put("vulnId", location + "|" + category + "|" + transition)
                .put("primaryLocationFull", location)
                .put("category", category);
        return new FoDAttributionIssue(node, transition);
    }

    private static FoDScanCondition condition(FoDAttributionCause cause, FoDAttributionDirection direction) {
        return new FoDScanCondition(cause, direction, List.of("evidence"));
    }

    private static FoDAttributionResult attribute(List<FoDScanCondition> conditions, List<IFoDIssueEvidenceMatcher> matchers,
            FoDAttributionIssue... issues) {
        var context = FoDAttributionContext.builder().changeThresholdPercent(10).metrics(Map.of()).issues(List.of(issues)).build();
        IFoDScanConditionDetector detector = c -> conditions;
        return new FoDAttributionEngine(List.of(detector), matchers).attribute(context);
    }

    private static void assertStatus(FoDAttributionIssue issue, String expected) {
        assertEquals(expected, issue.node().at("/attribution/status").asText(), issue.node().toString());
    }

    private static List<JsonNode> attributions(FoDAttributionIssue issue) {
        var result = new ArrayList<JsonNode>();
        issue.node().at("/attribution/attributions").forEach(result::add);
        return result;
    }
}
