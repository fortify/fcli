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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Previous and current value of a numeric scan metric, like a scan statistic or site tree statistic,
 * optionally with the original (unparsed) values for display purposes
 */
public record FoDAttributionMetric(Double previous, Double current, String previousDisplay, String currentDisplay) {
    private static final Pattern SIZE_PATTERN = Pattern.compile("(?i)^\\s*([\\d.,]+)\\s*([KMGT]?B)?\\s*$");
    private static final Map<String, Double> SIZE_UNITS = Map.of(
            "B", 1d, "KB", 1024d, "MB", 1024d * 1024, "GB", 1024d * 1024 * 1024, "TB", 1024d * 1024 * 1024 * 1024);

    public FoDAttributionMetric(Double previous, Double current) {
        this(previous, current, null, null);
    }

    public static FoDAttributionMetric of(JsonNode previous, JsonNode current) {
        return new FoDAttributionMetric(parse(previous), parse(current), getDisplay(previous), getDisplay(current));
    }

    public boolean isAvailable() {
        return previous != null && current != null;
    }

    public double getAbsoluteChange() {
        return isAvailable() ? Math.abs(current - previous) : 0;
    }

    /**
     * Determine whether the relative change, calculated as |current - previous| / max(|previous|, |current|),
     * meets the given threshold percentage.
     *
     * @return INCREASE or DECREASE if the threshold is met, null otherwise
     */
    public FoDAttributionDirection getDirection(int thresholdPercent) {
        if ( !isAvailable() ) { return null; }
        var max = Math.max(Math.abs(previous), Math.abs(current));
        var change = current - previous;
        if ( max == 0 || Math.abs(change) * 100 / max < thresholdPercent ) { return null; }
        return change > 0 ? FoDAttributionDirection.INCREASE : FoDAttributionDirection.DECREASE;
    }

    public String describe(String name) {
        return String.format("%s: %s -> %s", name,
                previousDisplay != null ? previousDisplay : format(previous),
                currentDisplay != null ? currentDisplay : format(current));
    }

    // Only textual values, like '85,404.77 KB', need to be retained for display; numbers are formatted consistently
    private static String getDisplay(JsonNode value) {
        return value != null && value.isTextual() ? value.asText() : null;
    }

    /** Parse numbers, numeric strings, and sizes like '85,404.77 KB' (returned in bytes) */
    static Double parse(JsonNode value) {
        if ( value == null || value.isNull() || value.isMissingNode() ) { return null; }
        if ( value.isNumber() ) { return value.asDouble(); }
        var matcher = SIZE_PATTERN.matcher(value.asText());
        if ( !matcher.matches() ) { return null; }
        var number = matcher.group(1).replace(",", "");
        if ( !NumberUtils.isParsable(number) ) { return null; }
        var unit = matcher.group(2) == null ? "B" : matcher.group(2).toUpperCase(Locale.ROOT);
        return Double.parseDouble(number) * SIZE_UNITS.get(unit);
    }

    private static String format(Double value) {
        if ( value == null ) { return "n/a"; }
        return value == Math.rint(value) && Math.abs(value) < 1e15
                ? String.valueOf(value.longValue())
                : String.format(Locale.ROOT, "%.4f", value);
    }
}
