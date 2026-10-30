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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Renders issues indeterminate if issues with the same category were both fixed and newly found or
 * reopened at the same, unchanged location. This may be caused by changes elsewhere in the data flow,
 * or by issue correlation; neither can be determined from the available data.
 */
public final class FoDAttributionPairedLocationMatcher implements IFoDIssueEvidenceMatcher {
    @Override
    public List<FoDIssueEvidence> match(FoDAttributionContext context, FoDAttributionIssue issue) {
        var pairs = context.getCached(PairIndex.class, () -> PairIndex.of(context.getIssues()));
        var location = getLocationKey(issue.getLocation());
        var category = StringUtils.defaultString(issue.getCategory());
        if ( location.isEmpty() || !pairs.isPaired(location, category) ) { return List.of(); }
        var changeIndex = context.getChangeIndex();
        if ( changeIndex != null && changeIndex.isLocationChanged(issue.getLocation()) ) { return List.of(); }
        return List.of(new FoDIssueEvidence(FoDAttributionCause.PAIRED_AT_UNCHANGED_LOCATION, String.format(
                "both fixed and new or reopened '%s' issues at location %s, which was not changed",
                category, issue.getLocation())));
    }

    private static String getLocationKey(String location) {
        var result = StringUtils.trimToEmpty(location).replace('\\', '/').toLowerCase(Locale.ROOT);
        return StringUtils.strip(result, "/");
    }

    private static final class PairIndex {
        // Location -> category -> [fixed count, new or reopened count]
        private final Map<String, Map<String, int[]>> counts = new HashMap<>();

        static PairIndex of(List<FoDAttributionIssue> issues) {
            var result = new PairIndex();
            for ( var issue : issues ) {
                var idx = issue.transition() == FoDAttributionTransition.FIXED ? 0 : 1;
                result.counts.computeIfAbsent(getLocationKey(issue.getLocation()), k -> new HashMap<>())
                        .computeIfAbsent(StringUtils.defaultString(issue.getCategory()), k -> new int[2])[idx]++;
            }
            return result;
        }

        boolean isPaired(String location, String category) {
            var c = counts.getOrDefault(location, Map.of()).get(category);
            return c != null && c[0] > 0 && c[1] > 0;
        }
    }
}
