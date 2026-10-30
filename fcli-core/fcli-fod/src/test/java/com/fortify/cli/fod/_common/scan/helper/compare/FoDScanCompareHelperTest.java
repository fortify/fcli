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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;

class FoDScanCompareHelperTest {
    private static final IFoDScanCompareSupport SAST = new FoDScanCompareSastSupport(1000, 25);

    @Test
    void limitedListRetainsFirstItemsInOrderAndCountsAll() {
        var list = new FoDScanCompareLimitedList<Integer>(3, Comparator.naturalOrder());
        List.of(9, 2, 7, 1, 5, 3).forEach(list::add);
        var result = JsonHelper.getObjectMapper().createObjectNode();
        list.addTo(result, "value", IntNode::valueOf);
        assertEquals(6, result.get("valueCount").asInt());
        assertEquals("[1,2,3]", result.get("values").toString());
        assertTrue(result.get("valuesTruncated").asBoolean());
    }

    @Test
    void limitedListNotTruncatedWithinLimit() {
        var list = new FoDScanCompareLimitedList<Integer>(3, Comparator.naturalOrder());
        List.of(2, 1).forEach(list::add);
        var result = JsonHelper.getObjectMapper().createObjectNode();
        list.addTo(result, "value", IntNode::valueOf);
        assertEquals("[1,2]", result.get("values").toString());
        assertFalse(result.get("valuesTruncated").asBoolean());
    }

    @Test
    void limitedListWithZeroMaximumOnlyCounts() {
        var list = new FoDScanCompareLimitedList<Integer>(0, Comparator.naturalOrder());
        List.of(2, 1).forEach(list::add);
        var result = JsonHelper.getObjectMapper().createObjectNode();
        list.addTo(result, "value", IntNode::valueOf);
        assertEquals(2, result.get("valueCount").asInt());
        assertEquals(0, result.get("values").size());
        assertTrue(result.get("valuesTruncated").asBoolean());
    }

    @Test
    void failsOnAnyFortifyImportedAssessmentType() {
        assertThrows(FcliSimpleException.class, () -> select(false,
                scan(4, "Completed", 98, "Some Future Type (Imported)"),
                scan(3, "Completed", 1, "Static Assessment"),
                scan(2, "Completed", 1, "Static Assessment")));
    }

    @Test
    void selectsLatestAndPreviousCompletedScanOfSameAssessmentType() {
        var selection = select(false,
                scan(5, "In_Progress", 1, "Static Assessment"),
                scan(4, "Completed", 1, "Static Assessment"),
                scan(3, "Completed", 1, "Static Assessment"));
        assertScanIds(selection, "4", "3");
    }

    @Test
    void failsOnCompletedScanOfOtherAssessmentTypeInBetween() {
        var e = assertThrows(FcliSimpleException.class, () -> select(false,
                scan(4, "Completed", 1, "Static Assessment"),
                scan(3, "Completed", 2, "Static+ Assessment"),
                scan(2, "Completed", 1, "Static Assessment")));
        assertTrue(e.getMessage().contains("scan 3"), e.getMessage());
    }

    @Test
    void failsOnImportedFortifyScanInBetween() {
        assertThrows(FcliSimpleException.class, () -> select(false,
                scan(4, "Completed", 1, "Static Assessment"),
                scan(3, "Completed", 99, "SCA (Imported)"),
                scan(2, "Completed", 1, "Static Assessment")));
    }

    @Test
    void failsIfMostRecentCompletedScanIsImportedFortifyScan() {
        var e = assertThrows(FcliSimpleException.class, () -> select(false,
                scan(4, "Completed", 99, "SCA (Imported)"),
                scan(3, "Completed", 1, "Static Assessment"),
                scan(2, "Completed", 1, "Static Assessment")));
        assertTrue(e.getMessage().contains("imported"), e.getMessage());
    }

    @Test
    void ignoresCanceledScansOfOtherAssessmentType() {
        var selection = select(false,
                scan(5, "Canceled", 2, "Static+ Assessment"),
                scan(4, "Completed", 1, "Static Assessment"),
                scan(3, "Canceled", 2, "Static+ Assessment"),
                scan(2, "Completed", 1, "Static Assessment"));
        assertScanIds(selection, "4", "2");
    }

    @Test
    void failsOnCanceledScansOfSameAssessmentTypeUnlessIgnored() {
        var newerCanceled = List.of(
                scan(5, "Canceled", 1, "Static Assessment"),
                scan(4, "Completed", 1, "Static Assessment"),
                scan(3, "Completed", 1, "Static Assessment"));
        var betweenCanceled = List.of(
                scan(5, "Completed", 1, "Static Assessment"),
                scan(4, "Canceled", 1, "Static Assessment"),
                scan(3, "Completed", 1, "Static Assessment"));
        assertThrows(FcliSimpleException.class, () -> FoDScanCompareHelper.selectScans(newerCanceled, SAST, false));
        assertThrows(FcliSimpleException.class, () -> FoDScanCompareHelper.selectScans(betweenCanceled, SAST, false));
        assertScanIds(FoDScanCompareHelper.selectScans(newerCanceled, SAST, true), "4", "3");
        assertScanIds(FoDScanCompareHelper.selectScans(betweenCanceled, SAST, true), "5", "3");
    }

    @Test
    void failsIfNoCompletedOrPreviousScan() {
        assertThrows(FcliSimpleException.class, () -> select(false));
        assertThrows(FcliSimpleException.class, () -> select(false, scan(1, "In_Progress", 1, "Static Assessment")));
        assertThrows(FcliSimpleException.class, () -> select(false, scan(1, "Completed", 1, "Static Assessment")));
    }

    @Test
    void parseManifestIgnoresTimestampColumns() {
        var previous = FoDScanCompareSastSupport.parseManifest(
                "\"/src/A.java\"  120  2026-01-01 10:00:00 AM\n\"/src/B.java\"  10  01/02/2026\n");
        var current = FoDScanCompareSastSupport.parseManifest(
                "\"/src/A.java\"  120  2026-02-03 11:30:15 PM\n\"/src/B.java\"  11  03/04/2026\n");
        assertEquals(previous.get("/src/A.java"), current.get("/src/A.java"));
        assertEquals("10", previous.get("/src/B.java"));
        assertEquals("11", current.get("/src/B.java"));
    }

    @Test
    void parseManifestNormalizesPathSeparators() {
        var previous = FoDScanCompareSastSupport.parseManifest("\"/site-packages\\yaml\\__init__.py\"  120\n");
        var current = FoDScanCompareSastSupport.parseManifest("\"/site-packages/yaml/__init__.py\"  120\n");
        assertEquals(previous, current);
    }

    @Test
    void requestLookupParsesRequestAndResponseLines() {
        var node = JsonHelper.getObjectMapper().createObjectNode()
                .put("requestContent", "OPTIONS / HTTP/1.1\r\nHost: zero.webappsecurity.com\r\n\r\n")
                .put("responseContent", "HTTP/1.1 501 Not Implemented\r\nServer: x\r\n\r\n");
        var info = FoDScanCompareIssueRequestLookup.parse(node);
        assertEquals("OPTIONS", info.method());
        assertEquals("501", info.statusCode());
    }

    @Test
    void traceLookupParsesTraceEntries() {
        var body = JsonHelper.getObjectMapper().createArrayNode();
        var entries = body.addObject().put("traceIndex", 0).putArray("traceEntries");
        entries.addObject().put("location", "src/A.java").put("lineNumber", 10).put("actionType", "InCall");
        entries.addObject().put("location", "").put("lineNumber", 11);
        entries.addObject().put("location", "src/B.java").put("lineNumber", 20);
        body.addObject().put("traceIndex", 1).putArray("traceEntries");
        var traces = FoDScanCompareIssueTraceLookup.parse(body);
        assertEquals(1, traces.size());
        assertEquals("src/A.java", traces.get(0).getSource().location());
        assertEquals(10, traces.get(0).getSource().lineNumber());
        assertEquals(3, traces.get(0).nodes().size());
        assertTrue(traces.get(0).getInterimNodes().isEmpty());
    }

    @Test
    void limitedListCountsItemsThatAreNotRetained() {
        var result = JsonHelper.getObjectMapper().createObjectNode();
        FoDScanCompareHelper.addValueList(result, "file", List.of("a", "b", "c"), 10, "b"::equals);
        assertEquals(3, result.get("fileCount").asInt());
        assertEquals("[\"b\"]", result.get("files").toString());
        assertFalse(result.get("filesTruncated").asBoolean());
        var limited = JsonHelper.getObjectMapper().createObjectNode();
        FoDScanCompareHelper.addValueList(limited, "file", List.of("a", "b", "c"), 1, v -> !"a".equals(v));
        assertEquals("[\"b\"]", limited.get("files").toString());
        assertTrue(limited.get("filesTruncated").asBoolean());
    }

    private static FoDScanCompareSelection select(boolean ignoreCanceled, JsonNode... scans) {
        return FoDScanCompareHelper.selectScans(List.of(scans), SAST, ignoreCanceled);
    }

    private static void assertScanIds(FoDScanCompareSelection selection, String targetScanId, String previousScanId) {
        assertEquals(targetScanId, FoDScanCompareHelper.getText(selection.target(), "scanId"));
        assertEquals(previousScanId, FoDScanCompareHelper.getText(selection.previous(), "scanId"));
    }

    private static JsonNode scan(int scanId, String status, int assessmentTypeId, String assessmentTypeName) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("scanId", scanId)
                .put("scanType", "Static")
                .put("analysisStatusType", status)
                .put("assessmentTypeId", assessmentTypeId)
                .put("assessmentTypeName", assessmentTypeName);
    }
}
