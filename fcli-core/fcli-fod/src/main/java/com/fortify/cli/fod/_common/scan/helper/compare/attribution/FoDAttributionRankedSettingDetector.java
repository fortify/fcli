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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Detects changes of a ranked setting, like scan policy. A higher rank indicates a broader scan;
 * changes from or to unranked values result in a change with unknown direction.
 */
public final class FoDAttributionRankedSettingDetector implements IFoDScanConditionDetector {
    private final FoDAttributionCause cause;
    private final String name;
    private final String jsonPointer;
    private final Map<String, Integer> ranks;

    /** @param ranks Rank per setting value; values are matched case-insensitively, ignoring non-alphanumeric characters */
    public FoDAttributionRankedSettingDetector(FoDAttributionCause cause, String name, String jsonPointer, Map<String, Integer> ranks) {
        this.cause = cause;
        this.name = name;
        this.jsonPointer = jsonPointer;
        this.ranks = ranks;
    }

    @Override
    public List<FoDScanCondition> detect(FoDAttributionContext context) {
        var previous = context.getPreviousText(jsonPointer);
        var current = context.getCurrentText(jsonPointer);
        if ( StringUtils.isBlank(previous) || StringUtils.isBlank(current) || normalize(previous).equals(normalize(current)) ) {
            return List.of();
        }
        var previousRank = ranks.get(normalize(previous));
        var currentRank = ranks.get(normalize(current));
        var direction = previousRank == null || currentRank == null || previousRank.equals(currentRank)
                ? FoDAttributionDirection.CHANGED
                : currentRank > previousRank ? FoDAttributionDirection.INCREASE : FoDAttributionDirection.DECREASE;
        return List.of(new FoDScanCondition(cause, direction, List.of(String.format("%s: %s -> %s", name, previous, current))));
    }

    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
