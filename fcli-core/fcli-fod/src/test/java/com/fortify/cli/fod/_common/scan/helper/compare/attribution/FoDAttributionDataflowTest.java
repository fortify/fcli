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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDIssueTrace.Node;

class FoDAttributionDataflowTest {
    private static final String CONTROLLER = "src/main/java/app/ProductController.java";
    private static final String SERVICE = "src/main/java/app/ProductService.java";
    private static final String REPOSITORY = "src/main/java/app/ProductRepository.java";

    @Test
    void sourceFileAdditionIsDirectAndInterimChangeIsInferred() {
        var lookup = new StubTraceLookup();
        var index = index(Map.of("/Src/" + SERVICE, "1", "/Src/" + REPOSITORY, "1"),
                Map.of("/Src/" + CONTROLLER, "1", "/Src/" + SERVICE, "2", "/Src/" + REPOSITORY, "1"));
        var issue = lookup.add(issue("v1", REPOSITORY, 135, "firstaid(1)", FoDAttributionTransition.NEW),
                trace(CONTROLLER, 97, CONTROLLER, 100, SERVICE, 106, REPOSITORY, 135));
        var context = context(index, lookup, issue);
        var evidence = causes(new FoDAttributionDataflowMatcher().match(context, issue));
        assertEquals(List.of(FoDAttributionCause.SOURCE_FILE_ADDED, FoDAttributionCause.DATAFLOW_FILE_CHANGED), evidence);
        assertEquals(Set.of("/Src/" + CONTROLLER, "/Src/" + SERVICE), context.getReferencedChanges());
        new FoDAttributionEngine(List.of(), List.of(new FoDAttributionDataflowMatcher())).attribute(context);
        assertStatus(issue, "DIRECT");
    }

    @Test
    void changedSourceFileIsInferredAndRemovedSourceFileExplainsFixedIssue() {
        var lookup = new StubTraceLookup();
        var index = index(Map.of("/Src/" + CONTROLLER, "1", "/Src/" + REPOSITORY, "1"), Map.of("/Src/" + REPOSITORY, "1"));
        var fixed = lookup.add(issue("v1", REPOSITORY, 135, "firstaid(1)", FoDAttributionTransition.FIXED),
                trace(CONTROLLER, 97, REPOSITORY, 135));
        var changedIndex = index(Map.of("/Src/" + CONTROLLER, "1", "/Src/" + REPOSITORY, "1"),
                Map.of("/Src/" + CONTROLLER, "2", "/Src/" + REPOSITORY, "1"));
        var matcher = new FoDAttributionDataflowMatcher();
        assertEquals(List.of(FoDAttributionCause.SOURCE_FILE_REMOVED), causes(matcher.match(context(index, lookup, fixed), fixed)));
        var context = context(changedIndex, lookup, fixed);
        new FoDAttributionEngine(List.of(), List.of(matcher)).attribute(context);
        assertStatus(fixed, "INFERRED");
    }

    @Test
    void tracesAreOnlyLookedUpForDataflowIssuesAtUnchangedSinkWithManifestChanges() {
        var lookup = new StubTraceLookup();
        var changed = index(Map.of("/Src/" + CONTROLLER, "1", "/Src/" + REPOSITORY, "1"),
                Map.of("/Src/" + CONTROLLER, "2", "/Src/" + REPOSITORY, "2"));
        var unchanged = index(Map.of("/Src/" + CONTROLLER, "1"), Map.of("/Src/" + CONTROLLER, "1"));
        var matcher = new FoDAttributionDataflowMatcher();
        var noSource = lookup.add(issue("v1", REPOSITORY, 10, "<None>", FoDAttributionTransition.NEW), trace(CONTROLLER, 1, REPOSITORY, 10));
        var sinkChanged = lookup.add(issue("v2", REPOSITORY, 10, "f(0)", FoDAttributionTransition.NEW), trace(CONTROLLER, 1, REPOSITORY, 10));
        var noChanges = lookup.add(issue("v3", CONTROLLER, 10, "f(0)", FoDAttributionTransition.NEW), trace(SERVICE, 1, CONTROLLER, 10));
        assertTrue(matcher.match(context(changed, lookup, noSource), noSource).isEmpty());
        assertTrue(matcher.match(context(changed, lookup, sinkChanged), sinkChanged).isEmpty());
        assertTrue(matcher.match(context(unchanged, lookup, noChanges), noChanges).isEmpty());
        assertEquals(0, lookup.getLookupCount());
    }

    @Test
    void pairWithIdenticalSourceAndSinkIsCorrelationError() {
        // From IWA-DotNet: fixed and new XSS issues with identical traces, without any manifest changes
        var lookup = new StubTraceLookup();
        var index = index(Map.of("/Src/Index.cshtml", "1"), Map.of("/Src/Index.cshtml", "1"));
        var fixed = lookup.add(issue("v1", "Index.cshtml", 47, "get_Search(return)", FoDAttributionTransition.FIXED),
                trace("Index.cshtml", 47, "Index.cshtml", 47));
        // Interim step differences don't affect correlation
        var newIssue = lookup.add(issue("v2", "Index.cshtml", 47, "get_Search(return)", FoDAttributionTransition.NEW),
                trace("Index.cshtml", 47, "Other.cs", 5, "Index.cshtml", 47));
        var other = lookup.add(issue("v3", "Index.cshtml", 60, "get_Keywords(return)", FoDAttributionTransition.NEW),
                trace("Index.cshtml", 60, "Index.cshtml", 60));
        attribute(index, lookup, fixed, newIssue, other);
        assertStatus(fixed, "INFERRED");
        assertCauses(fixed, "INSTANCE_ID_CORRELATION_ERROR");
        assertStatus(newIssue, "INFERRED");
        assertStatus(other, "INDETERMINATE");
        assertCauses(other);
    }

    @Test
    void nonDataflowPairWithIdenticalSinkIsCorrelationError() {
        var fixed = issue("v1", "data.sql", 12, "<None>", FoDAttributionTransition.FIXED);
        var newIssue = issue("v2", "data.sql", 12, "<None>", FoDAttributionTransition.NEW);
        var lookup = new StubTraceLookup();
        attribute(null, lookup, fixed, newIssue);
        assertCauses(fixed, "INSTANCE_ID_CORRELATION_ERROR");
        assertEquals(0, lookup.getLookupCount());
    }

    @Test
    void pairWithChangedDataflowFileIsNotIndeterminate() {
        var lookup = new StubTraceLookup();
        var index = index(Map.of("/Src/" + SERVICE, "1", "/Src/" + REPOSITORY, "1"),
                Map.of("/Src/" + SERVICE, "2", "/Src/" + REPOSITORY, "1"));
        var fixed = lookup.add(issue("v1", REPOSITORY, 135, "firstaid(1)", FoDAttributionTransition.FIXED),
                trace(CONTROLLER, 97, SERVICE, 106, REPOSITORY, 135));
        var newIssue = lookup.add(issue("v2", REPOSITORY, 135, "search(1)", FoDAttributionTransition.NEW),
                trace(CONTROLLER, 120, SERVICE, 110, REPOSITORY, 135));
        attribute(index, lookup, fixed, newIssue);
        assertStatus(fixed, "INFERRED");
        assertCauses(fixed, "DATAFLOW_FILE_CHANGED");
        assertStatus(newIssue, "INFERRED");
    }

    private static FoDAttributionIssue issue(String vulnId, String location, int lineNumber, String source,
            FoDAttributionTransition transition) {
        var node = JsonHelper.getObjectMapper().createObjectNode()
                .put("vulnId", vulnId).put("primaryLocationFull", location).put("lineNumber", lineNumber)
                .put("category", "Category").put("source", source);
        return new FoDAttributionIssue(node, transition);
    }

    private static FoDIssueTrace trace(Object... locationsAndLines) {
        var nodes = new ArrayList<Node>();
        for ( int i = 0; i < locationsAndLines.length; i += 2 ) {
            nodes.add(new Node((String)locationsAndLines[i], (Integer)locationsAndLines[i + 1]));
        }
        return new FoDIssueTrace(nodes);
    }

    private static FoDAttributionFileChangeIndex index(Map<String, String> previous, Map<String, String> current) {
        return new FoDAttributionFileChangeIndex(new LinkedHashMap<>(previous), new LinkedHashMap<>(current));
    }

    private static FoDAttributionContext context(FoDAttributionFileChangeIndex index, IFoDIssueTraceLookup lookup,
            FoDAttributionIssue... issues) {
        return FoDAttributionContext.builder().changeThresholdPercent(10).changeIndex(index).traceLookup(lookup)
                .issues(List.of(issues)).build();
    }

    private static void attribute(FoDAttributionFileChangeIndex index, IFoDIssueTraceLookup lookup, FoDAttributionIssue... issues) {
        new FoDAttributionEngine(List.of(), List.of(new FoDAttributionFileChangeMatcher(), new FoDAttributionDataflowMatcher(),
                new FoDAttributionCorrelationErrorMatcher())).attribute(context(index, lookup, issues));
    }

    private static List<FoDAttributionCause> causes(List<FoDIssueEvidence> evidence) {
        return evidence.stream().map(FoDIssueEvidence::cause).toList();
    }

    private static void assertStatus(FoDAttributionIssue issue, String expected) {
        assertEquals(expected, issue.node().at("/attribution/status").asText(), issue.node().toString());
    }

    private static void assertCauses(FoDAttributionIssue issue, String... expected) {
        var actual = new ArrayList<String>();
        issue.node().at("/attribution/attributions").forEach(a -> actual.add(a.path("cause").asText()));
        assertEquals(List.of(expected), actual, issue.node().toString());
    }

    private static final class StubTraceLookup implements IFoDIssueTraceLookup {
        private final Map<String, List<FoDIssueTrace>> traces = new HashMap<>();
        private int count;

        FoDAttributionIssue add(FoDAttributionIssue issue, FoDIssueTrace... issueTraces) {
            traces.put(issue.getText("vulnId"), List.of(issueTraces));
            return issue;
        }

        @Override
        public List<FoDIssueTrace> lookup(FoDAttributionIssue issue) {
            count++;
            return traces.get(issue.getText("vulnId"));
        }

        @Override
        public int getLookupCount() {
            return count;
        }

        @Override
        public boolean isLimitReached() {
            return false;
        }
    }
}
