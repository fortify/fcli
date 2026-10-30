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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionMetricDetector.MetricRule;

class FoDAttributionDetectorTest {
    private static final String DAST = "/dynamicScanSummaryDetails/";

    @Test
    void packagingShrinkIsDetected() {
        var detector = new FoDAttributionMetricDetector(FoDAttributionCause.PACKAGING_CHANGE,
                MetricRule.size("fileCount"), MetricRule.size("totalLinesOfCode"), MetricRule.size("payloadSize"));
        var conditions = detector.detect(context(10, Map.of(
                "fileCount", new FoDAttributionMetric(6431d, 1d),
                "totalLinesOfCode", new FoDAttributionMetric(1742670d, 193d),
                "payloadSize", FoDAttributionMetric.of(TextNode.valueOf("101,226.30 KB"), TextNode.valueOf("101,226.30 KB")))));
        assertEquals(1, conditions.size());
        assertEquals(FoDAttributionDirection.DECREASE, conditions.get(0).direction());
        assertEquals(List.of("fileCount: 6431 -> 1", "totalLinesOfCode: 1742670 -> 193"), conditions.get(0).evidence());
    }

    @Test
    void metricChangesBelowThresholdAreIgnored() {
        var detector = new FoDAttributionMetricDetector(FoDAttributionCause.PACKAGING_CHANGE, MetricRule.size("fileCount"));
        assertTrue(detector.detect(context(10, Map.of("fileCount", new FoDAttributionMetric(100d, 91d)))).isEmpty());
        assertEquals(1, detector.detect(context(10, Map.of("fileCount", new FoDAttributionMetric(100d, 90d)))).size());
        assertTrue(detector.detect(context(20, Map.of("fileCount", new FoDAttributionMetric(100d, 90d)))).isEmpty());
    }

    @Test
    void mixedMetricDirectionsResultInUnknownDirection() {
        var detector = new FoDAttributionMetricDetector(FoDAttributionCause.PACKAGING_CHANGE,
                MetricRule.size("fileCount"), MetricRule.size("totalLinesOfCode"));
        var conditions = detector.detect(context(10, Map.of(
                "fileCount", new FoDAttributionMetric(100d, 50d),
                "totalLinesOfCode", new FoDAttributionMetric(1000d, 2000d))));
        assertEquals(FoDAttributionDirection.CHANGED, conditions.get(0).direction());
    }

    @Test
    void smallCounterChangesAreIgnored() {
        var detector = new FoDAttributionMetricDetector(FoDAttributionCause.AUTHENTICATION_CHANGE, MetricRule.degradationCounter("logouts"));
        assertTrue(detector.detect(context(10, Map.of("logouts", new FoDAttributionMetric(0d, 3d)))).isEmpty());
        var conditions = detector.detect(context(10, Map.of("logouts", new FoDAttributionMetric(0d, 25d))));
        assertEquals(FoDAttributionDirection.DECREASE, conditions.get(0).direction());
    }

    @Test
    void ratioMetricsCompensateForScanSize() {
        // From the zero.webappsecurity.com sample: fewer failed requests, but only because there were fewer crawl sessions
        var detector = new FoDAttributionMetricDetector(FoDAttributionCause.AVAILABILITY_CHANGE,
                MetricRule.degradationRatio("failedRequests", "crawlSessions"));
        var metrics = Map.of(
                "failedRequests", new FoDAttributionMetric(29d, 10d),
                "crawlSessions", new FoDAttributionMetric(348d, 109d));
        assertTrue(detector.detect(context(10, metrics)).isEmpty());
        var degraded = Map.of(
                "failedRequests", new FoDAttributionMetric(29d, 100d),
                "crawlSessions", new FoDAttributionMetric(348d, 340d));
        assertEquals(FoDAttributionDirection.DECREASE, detector.detect(context(10, degraded)).get(0).direction());
    }

    @Test
    void rankedSettingDirection() {
        var detector = new FoDAttributionRankedSettingDetector(FoDAttributionCause.SCAN_POLICY_CHANGE, "policyName", DAST + "policyName",
                Map.of("standard", 3, "criticalsandhighs", 2, "passivescan", 1));
        var narrowed = detector.detect(context(dast("policyName", "Standard"), dast("policyName", "Criticals and Highs")));
        assertEquals(FoDAttributionDirection.DECREASE, narrowed.get(0).direction());
        assertEquals("policyName: Standard -> Criticals and Highs", narrowed.get(0).evidence().get(0));
        var broadened = detector.detect(context(dast("policyName", "Passive Scan"), dast("policyName", "Standard")));
        assertEquals(FoDAttributionDirection.INCREASE, broadened.get(0).direction());
        var unranked = detector.detect(context(dast("policyName", "Standard"), dast("policyName", "Custom")));
        assertEquals(FoDAttributionDirection.CHANGED, unranked.get(0).direction());
        assertTrue(detector.detect(context(dast("policyName", "Standard"), dast("policyName", "standard"))).isEmpty());
        assertTrue(detector.detect(context(json(), dast("policyName", "Standard"))).isEmpty());
    }

    @Test
    void booleanSettingDirection() {
        var detector = new FoDAttributionBooleanSettingDetector(FoDAttributionCause.SCOPE_CHANGE,
                new FoDAttributionBooleanSettingDetector.Setting("restrict", DAST + "restrict", false),
                new FoDAttributionBooleanSettingDetector.Setting("forms", DAST + "forms", true));
        var narrowed = detector.detect(context(dast("restrict", false), dast("restrict", true)));
        assertEquals(FoDAttributionDirection.DECREASE, narrowed.get(0).direction());
        var mixed = detector.detect(context(with(dast("restrict", false), "forms", false), with(dast("restrict", true), "forms", true)));
        assertEquals(FoDAttributionDirection.CHANGED, mixed.get(0).direction());
    }

    @Test
    void targetUrlChangeIgnoresEquivalentUrls() {
        var detector = new FoDAttributionChangedSettingDetector(FoDAttributionCause.TARGET_CHANGE, FoDAttributionUrls::normalizeKey,
                new FoDAttributionChangedSettingDetector.Setting("dynamicSiteURL", DAST + "dynamicSiteURL"));
        assertTrue(detector.detect(context(dast("dynamicSiteURL", "http://Host:80/"), dast("dynamicSiteURL", "http://host"))).isEmpty());
        assertEquals(1, detector.detect(context(dast("dynamicSiteURL", "http://a/"), dast("dynamicSiteURL", "http://b/"))).size());
    }

    @Test
    void timeboxDetection() {
        var detector = new FoDAttributionTimeboxDetector(DAST + "timeBoxInHours", DAST + "elapsedTimeSpan");
        // Current scan hit its timebox
        var hit = detector.detect(context(timebox(2, "00:30:00"), timebox(2, "01:55:00")));
        assertEquals(FoDAttributionDirection.DECREASE, hit.get(0).direction());
        // Previous scan hit its timebox, current scan didn't
        var recovered = detector.detect(context(timebox(2, "01:59:00"), timebox(4, "02:30:00")));
        assertEquals(FoDAttributionDirection.INCREASE, recovered.get(0).direction());
        // Neither scan hit its timebox
        assertTrue(detector.detect(context(timebox(2, "00:30:00"), timebox(2, "00:40:00"))).isEmpty());
        // Timebox reduced
        assertEquals(FoDAttributionDirection.DECREASE,
                detector.detect(context(timebox(4, "00:30:00"), timebox(2, "00:40:00"))).get(0).direction());
        // A higher change threshold must not cause scans far below their timebox to be considered limited
        var highThreshold = FoDAttributionContext.builder().changeThresholdPercent(100)
                .previousSummary(timebox(2, "00:30:00")).currentSummary(timebox(2, "00:40:00")).build();
        assertTrue(detector.detect(highThreshold).isEmpty());
        assertEquals(26.5, FoDAttributionTimeboxDetector.parseTimeSpanHours("1.02:30:00"), 0.0001);
    }

    @Test
    void massRelocationDetection() {
        var previous = new LinkedHashMap<String, String>();
        var current = new LinkedHashMap<String, String>();
        for ( int i = 0; i < 20; i++ ) {
            previous.put("/old/File" + i + ".java", String.valueOf(i + 100));
            current.put("/new/File" + i + ".java", String.valueOf(i + 100));
        }
        var index = new FoDAttributionFileChangeIndex(previous, current);
        var context = FoDAttributionContext.builder().changeThresholdPercent(10).changeIndex(index).build();
        var conditions = new FoDAttributionMassRelocationDetector().detect(context);
        assertEquals(FoDAttributionCause.MASS_RELOCATION, conditions.get(0).cause());
        assertEquals("20 of 20 files were moved to a different location", conditions.get(0).evidence().get(0));
    }

    @Test
    void metricParsesSizes() {
        var detector = new FoDAttributionMetricDetector(FoDAttributionCause.PACKAGING_CHANGE, MetricRule.size("payloadSize"));
        var payload = detector.detect(context(10, Map.of("payloadSize",
                FoDAttributionMetric.of(TextNode.valueOf("6.72 KB"), TextNode.valueOf("101,226.30 KB")))));
        assertEquals("payloadSize: 6.72 KB -> 101,226.30 KB", payload.get(0).evidence().get(0));
        assertEquals(85404.77 * 1024, FoDAttributionMetric.parse(TextNode.valueOf("85,404.77 KB")), 0.01);
        assertEquals(2.0 * 1024 * 1024, FoDAttributionMetric.parse(TextNode.valueOf("2 MB")), 0.01);
        assertEquals(236, FoDAttributionMetric.parse(TextNode.valueOf("236")), 0.01);
    }

    private static FoDAttributionContext context(int threshold, Map<String, FoDAttributionMetric> metrics) {
        return FoDAttributionContext.builder().changeThresholdPercent(threshold).metrics(metrics).build();
    }

    private static FoDAttributionContext context(ObjectNode previous, ObjectNode current) {
        return FoDAttributionContext.builder().changeThresholdPercent(10).previousSummary(previous).currentSummary(current).build();
    }

    private static ObjectNode json() {
        return JsonHelper.getObjectMapper().createObjectNode();
    }

    private static ObjectNode dast(String name, String value) {
        var result = json();
        result.putObject("dynamicScanSummaryDetails").put(name, value);
        return result;
    }

    private static ObjectNode dast(String name, boolean value) {
        var result = json();
        result.putObject("dynamicScanSummaryDetails").put(name, value);
        return result;
    }

    private static ObjectNode with(ObjectNode summary, String name, boolean value) {
        ((ObjectNode)summary.get("dynamicScanSummaryDetails")).put(name, value);
        return summary;
    }

    private static ObjectNode timebox(int hours, String elapsed) {
        var result = json();
        result.putObject("dynamicScanSummaryDetails").put("timeBoxInHours", hours).put("elapsedTimeSpan", elapsed);
        return result;
    }
}
