/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class DateTimePeriodHelperTest {
    // Use full range including weeks/months/years now supported
    private static final DateTimePeriodHelper ALL = DateTimePeriodHelper.all();
    private static final long DAY_MS = 24L*60L*60L*1000L;

    @Test @DisplayName("Parses milliseconds unit 'ms'")
    void parsesMilliseconds() {
        assertEquals(500, ALL.parsePeriodToMillis("500ms"));
        assertEquals(0, ALL.parsePeriodToMillis("0ms"));
        assertEquals(1500, ALL.parsePeriodToMillis("1s500ms"));
    }

    @Test @DisplayName("Parses mixed units and sums correctly")
    void parsesMixed() {
        assertEquals(61000, ALL.parsePeriodToMillis("1m1s"));
        assertEquals(62500, ALL.parsePeriodToMillis("1m2s500ms"));
    }

    @Test @DisplayName("Rejects invalid leftover characters")
    void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, ()->ALL.parsePeriodToMillis("10xm"));
        assertThrows(IllegalArgumentException.class, ()->ALL.parsePeriodToMillis("10msXYZ"));
        assertThrows(IllegalArgumentException.class, ()->ALL.parsePeriodToMillis("ms"));
    }

    @Test @DisplayName("Parses minutes unaffected by ms handling")
    void parsesMinutesDistinctFromMs() {
        assertEquals(60000, ALL.parsePeriodToMillis("1m"));
        assertEquals(120000, ALL.parsePeriodToMillis("2m"));
    }

    // --- Extended units (merged from DateTimePeriodHelperExtendedTest) ---
    @Test @DisplayName("Parses weeks")
    void parsesWeeks() {
        assertEquals(2 * 7L * DAY_MS, ALL.parsePeriodToMillis("2w"));
        assertEquals(7L * DAY_MS + 3 * DAY_MS, ALL.parsePeriodToMillis("1w3d"));
    }

    @Test @DisplayName("Parses months (approximate 30 days)")
    void parsesMonths() {
        assertEquals(30L * DAY_MS, ALL.parsePeriodToMillis("1M"));
        assertEquals(2 * 30L * DAY_MS + 5 * DAY_MS, ALL.parsePeriodToMillis("2M5d"));
    }

    @Test @DisplayName("Parses years (approximate 365 days) and mixes with other units")
    void parsesYearsMixed() {
        assertEquals(365L * DAY_MS, ALL.parsePeriodToMillis("1y"));
        long expected = 365L*DAY_MS + 2*30L*DAY_MS + 1*7L*DAY_MS + 4*DAY_MS + 12L*60L*60L*1000L + 5L*60L*1000L + 6L*1000L + 250L; // 1y2M1w4d12h5m6s250ms
        assertEquals(expected, ALL.parsePeriodToMillis("1y2M1w4d12h5m6s250ms"));
    }

    @Test @DisplayName("Rejects invalid estimated unit segment")
    void rejectsInvalidEstimated() {
        assertThrows(IllegalArgumentException.class, ()->ALL.parsePeriodToMillis("1Q"));
    }

    @Test @DisplayName("All range includes years")
    void allRangeIncludesYears() {
        assertTrue(ALL.parsePeriodToMillis("1y") > 0);
    }
}
