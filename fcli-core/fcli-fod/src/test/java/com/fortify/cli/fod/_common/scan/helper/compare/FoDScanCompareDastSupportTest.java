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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.scan.helper.compare.FoDScanCompareDastSupport.SiteTree;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionContext;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionEngine;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionIssue;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionTransition;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeMatcher;

class FoDScanCompareDastSupportTest {
    private static final FoDScanCompareDastSupport DAST = new FoDScanCompareDastSupport(1000, 25);

    @Test
    void equivalentUrlsAreNotReportedAsAddedAndRemoved() {
        var changes = compare(
                List.of(entry("https", "App.example.com", "443", "/shop/", "GET", 200)),
                List.of(entry("https", "app.example.com", "", "/shop", "GET", 200)));
        var result = addTo(changes, change -> true);
        assertEquals(0, result.get("addedUrlCount").asInt());
        assertEquals(0, result.get("removedUrlCount").asInt());
        assertEquals(0, result.get("changedUrlCount").asInt());
        assertTrue(changes.getChangeSets().stream().allMatch(s -> s.items().isEmpty()));
        assertEquals(List.of(1L, 1L), statistic(result, "urlCount"));
    }

    @Test
    void equivalentUrlsWithDifferentMethodsAreReportedAsChanged() {
        var changes = compare(
                List.of(entry("http", "host", "80", "/api/", "GET", 200)),
                List.of(entry("http", "host", "", "/api", "GET", 200), entry("http", "host", "", "/api", "POST", 500)));
        var result = addTo(changes, change -> true);
        assertEquals(0, result.get("addedUrlCount").asInt());
        assertEquals(0, result.get("removedUrlCount").asInt());
        assertEquals(1, result.get("changedUrlCount").asInt());
        var changed = result.get("changedUrls").get(0);
        assertEquals("http://host/api", changed.get("url").asText());
        assertEquals("[\"GET\"]", changed.get("previousMethods").toString());
        assertEquals("[\"GET\",\"POST\"]", changed.get("currentMethods").toString());
    }

    @Test
    void limitedChangeDetailsListUrlsReferencedByAttributions() {
        var changes = compare(
                List.of(entry("http", "host", "", "/removed", "GET", 200), entry("http", "host", "", "/other-removed", "GET", 200)),
                List.of(entry("http", "host", "", "/added/", "GET", 200), entry("http", "host", "", "/other-added", "GET", 200)));
        // Issue locations don't need to be in the same form as site tree URLs
        var newIssue = issue("http://HOST:80/added", FoDAttributionTransition.NEW);
        var fixedIssue = issue("/removed/", FoDAttributionTransition.FIXED);
        var context = FoDAttributionContext.builder().changeThresholdPercent(10).changeIndex(changes.getChangeIndex())
                .issues(List.of(newIssue, fixedIssue)).build();
        var attributionResult = new FoDAttributionEngine(List.of(), List.of(new FoDAttributionUrlChangeMatcher())).attribute(context);
        var result = addTo(changes, FoDScanCompareHelper.getChangeFilter(true, attributionResult));
        assertEquals(List.of("http://host/added/"), urls(result.get("addedUrls")));
        assertEquals(List.of("http://host/removed"), urls(result.get("removedUrls")));
        assertEquals(2, result.get("addedUrlCount").asInt());
        assertEquals(2, result.get("removedUrlCount").asInt());
        assertFalse(result.get("addedUrlsTruncated").asBoolean());
    }

    @Test
    void urlChangesAreSkippedForTooLargeSiteTree() {
        var changes = DAST.compareSiteTrees("1", SiteTree.TOO_LARGE, "2", SiteTree.of(List.of(entry("http", "host", "", "/", "GET", 200))));
        var result = addTo(changes, change -> true);
        assertNull(changes.getChangeIndex());
        assertTrue(changes.getSkipReason().contains("--max-site-tree-size"));
        assertTrue(result.get("siteTreeComparisonSkipped").asBoolean());
        assertFalse(result.has("addedUrls"));
        assertTrue(changes.getChangeSets().isEmpty());
    }

    @Test
    void invalidLimitsAreRejected() {
        assertThrows(FcliSimpleException.class, () -> new FoDScanCompareDastSupport(-1, 25));
        assertThrows(FcliSimpleException.class, () -> new FoDScanCompareDastSupport(1000, 0));
        assertThrows(FcliSimpleException.class, () -> new FoDScanCompareSastSupport(-1, 25));
        assertThrows(FcliSimpleException.class, () -> new FoDScanCompareSastSupport(1000, 0));
    }

    private static IFoDScanCompareChanges compare(List<JsonNode> previous, List<JsonNode> current) {
        return DAST.compareSiteTrees("1", SiteTree.of(previous), "2", SiteTree.of(current));
    }

    private static ObjectNode addTo(IFoDScanCompareChanges changes, Predicate<String> includeChange) {
        var result = JsonHelper.getObjectMapper().createObjectNode();
        changes.addTo(result, includeChange);
        return result;
    }

    private static JsonNode entry(String scheme, String host, String port, String path, String method, int statusCode) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("scheme", scheme).put("host", host).put("port", port).put("path", path)
                .put("method", method).put("statusCode", statusCode);
    }

    private static FoDAttributionIssue issue(String location, FoDAttributionTransition transition) {
        return new FoDAttributionIssue(JsonHelper.getObjectMapper().createObjectNode().put("primaryLocationFull", location), transition);
    }

    private static List<String> urls(JsonNode array) {
        var result = new ArrayList<String>();
        array.forEach(n -> result.add(n.get("url").asText()));
        return result;
    }

    private static List<Long> statistic(ObjectNode result, String name) {
        for ( var statistic : result.get("siteTreeStatistics") ) {
            if ( name.equals(statistic.get("name").asText()) ) {
                return List.of(statistic.get("previousValue").asLong(), statistic.get("currentValue").asLong());
            }
        }
        return List.of();
    }
}
