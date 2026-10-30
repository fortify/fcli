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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Detects DAST scan timebox limitations. A scan is considered to have hit its timebox if the
 * elapsed scan time is within a fixed margin of the timebox; this margin is independent of the
 * change threshold, as a higher change threshold should never result in more conditions. The
 * current scan is considered limited if it hit its timebox or if the timebox was reduced; if
 * only the previous scan hit its timebox, the current scan is considered to have recovered.
 */
public final class FoDAttributionTimeboxDetector implements IFoDScanConditionDetector {
    static final double TIMEBOX_REACHED_MARGIN_PERCENT = 10;
    // .NET TimeSpan format, like '00:03:18', '1.02:03:04' or '00:03:18.1234567'
    private static final Pattern TIME_SPAN_PATTERN = Pattern.compile("^(?:(\\d+)\\.)?(\\d+):(\\d+):(\\d+)(?:\\.\\d+)?$");
    private final String timeboxPointer;
    private final String elapsedPointer;

    public FoDAttributionTimeboxDetector(String timeboxPointer, String elapsedPointer) {
        this.timeboxPointer = timeboxPointer;
        this.elapsedPointer = elapsedPointer;
    }

    @Override
    public List<FoDScanCondition> detect(FoDAttributionContext context) {
        var previousTimebox = toHours(context.getPreviousValue(timeboxPointer));
        var currentTimebox = toHours(context.getCurrentValue(timeboxPointer));
        var previousElapsed = parseTimeSpanHours(context.getPreviousText(elapsedPointer));
        var currentElapsed = parseTimeSpanHours(context.getCurrentText(elapsedPointer));
        var evidence = new ArrayList<String>();
        var reduced = currentTimebox != null && (previousTimebox == null || currentTimebox < previousTimebox);
        if ( reduced ) { evidence.add(String.format("timeBoxInHours: %s -> %s", format(previousTimebox), format(currentTimebox))); }
        if ( isHit(currentTimebox, currentElapsed) ) {
            evidence.add(String.format("current scan duration %s reached timebox of %s hours",
                    context.getCurrentText(elapsedPointer), format(currentTimebox)));
        }
        if ( !evidence.isEmpty() ) {
            return List.of(new FoDScanCondition(FoDAttributionCause.TIMEBOX_CHANGE, FoDAttributionDirection.DECREASE, evidence));
        }
        if ( isHit(previousTimebox, previousElapsed) ) {
            return List.of(new FoDScanCondition(FoDAttributionCause.TIMEBOX_CHANGE, FoDAttributionDirection.INCREASE,
                    List.of(String.format("previous scan duration %s reached timebox of %s hours",
                            context.getPreviousText(elapsedPointer), format(previousTimebox)))));
        }
        return List.of();
    }

    private static boolean isHit(Double timeboxHours, Double elapsedHours) {
        return timeboxHours != null && timeboxHours > 0 && elapsedHours != null
                && elapsedHours >= timeboxHours * (100 - TIMEBOX_REACHED_MARGIN_PERCENT) / 100;
    }

    private static Double toHours(JsonNode value) {
        return value.isNumber() && value.asDouble() > 0 ? value.asDouble() : null;
    }

    static Double parseTimeSpanHours(String value) {
        if ( value == null ) { return null; }
        var matcher = TIME_SPAN_PATTERN.matcher(value.trim());
        if ( !matcher.matches() ) { return null; }
        var days = matcher.group(1) == null ? 0 : Long.parseLong(matcher.group(1));
        return days * 24 + Long.parseLong(matcher.group(2))
                + Long.parseLong(matcher.group(3)) / 60d + Long.parseLong(matcher.group(4)) / 3600d;
    }

    private static String format(Double hours) {
        return hours == null ? "none" : hours == Math.rint(hours) ? String.valueOf(hours.longValue()) : String.format(Locale.ROOT, "%.2f", hours);
    }
}
