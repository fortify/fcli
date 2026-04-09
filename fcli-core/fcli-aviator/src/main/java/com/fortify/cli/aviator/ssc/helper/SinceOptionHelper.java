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
package com.fortify.cli.aviator.ssc.helper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JSONDateTimeConverter;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;

/**
 * Helper for parsing the --since option value into an OffsetDateTime cutoff.
 * Supports relative durations (e.g. 7d, 2w, 1m, 90d) and absolute date strings
 * (e.g. 2025-01-01, 2025-01-01T10:30:00, 2025-01-01T10:30:00Z).
 */
public final class SinceOptionHelper {
    private static final DateTimePeriodHelper PERIOD_HELPER =
            DateTimePeriodHelper.byRange(Period.DAYS, Period.YEARS);

    private SinceOptionHelper() {}

    /**
     * Parses the given --since value to an OffsetDateTime.
     * Returns null if sinceValue is null or blank (meaning no filter).
     * Tries relative period parsing first, then falls back to absolute date parsing.
     *
     * @param sinceValue the raw --since option value
     * @return resolved OffsetDateTime cutoff, or null if sinceValue is blank
     * @throws FcliSimpleException if the value cannot be parsed
     */
    public static OffsetDateTime parse(String sinceValue) {
        if (sinceValue == null || sinceValue.isBlank()) {
            return null;
        }
        try {
            return PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod(sinceValue);
        } catch (Exception relativeEx) {
            try {
                return new JSONDateTimeConverter()
                        .parseZonedDateTime(sinceValue)
                        .toOffsetDateTime()
                        .withOffsetSameInstant(ZoneOffset.UTC);
            } catch (Exception absoluteEx) {
                throw new FcliSimpleException(
                    "Invalid --since value: '" + sinceValue + "'. " +
                    "Use a relative duration (e.g. 7d, 2w, 1M, 90d) or an absolute date " +
                    "(e.g. 2025-01-01, 2025-01-01T10:30:00, 2025-01-01T10:30:00Z)."
                );
            }
        }
    }

    /**
     * Returns true if the given uploadDate string represents a date that is
     * on or after the given cutoff. If cutoff is null, always returns true.
     *
     * @param uploadDateStr the artifact uploadDate string from SSC (ISO 8601)
     * @param cutoff        the resolved --since cutoff, or null for no filter
     * @return true if the artifact should be included
     */
    public static boolean isOnOrAfter(String uploadDateStr, OffsetDateTime cutoff) {
        if (cutoff == null) {
            return true;
        }
        if (uploadDateStr == null || uploadDateStr.isBlank()) {
            return false;
        }
        try {
            OffsetDateTime uploadDate = new JSONDateTimeConverter()
                    .parseZonedDateTime(uploadDateStr)
                    .toOffsetDateTime()
                    .withOffsetSameInstant(ZoneOffset.UTC);
            return !uploadDate.isBefore(cutoff);
        } catch (Exception e) {
            return false;
        }
    }
}
