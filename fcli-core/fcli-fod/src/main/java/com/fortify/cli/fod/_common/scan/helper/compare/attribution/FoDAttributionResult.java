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
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Scan-level attribution results; issue-level attributions are stored on the individual issues */
public final class FoDAttributionResult {
    private final FoDAttributionContext context;
    private final List<FoDScanCondition> conditions;
    private final FoDAttributionSummary summary;

    FoDAttributionResult(FoDAttributionContext context, List<FoDScanCondition> conditions, FoDAttributionSummary summary) {
        this.context = context;
        this.conditions = conditions;
        this.summary = summary;
    }

    public List<FoDScanCondition> getConditions() {
        return conditions;
    }

    /** @return Number of issues per cause that determined their attribution status, ordered by descending count */
    public Map<FoDAttributionCause, Integer> getDriverCauses(FoDAttributionTransition transition) {
        return summary.getDriverCauses(transition);
    }

    public int getIndeterminateCount(FoDAttributionTransition transition) {
        return summary.getStatusCount(transition, FoDAttributionStatus.INDETERMINATE);
    }

    /** @return Changes (file paths or URLs as listed in the compare output) referenced by any issue attribution */
    public Set<String> getReferencedChanges() {
        return context.getReferencedChanges();
    }

    public void addTo(ObjectNode result) {
        result.put("attributionEnabled", true);
        result.put("changeThresholdPercent", context.getChangeThresholdPercent());
        result.put("scanConditionCount", conditions.size());
        var conditionArray = result.putArray("scanConditions");
        conditions.stream().map(FoDScanCondition::toJson).forEach(conditionArray::add);
        result.set("attributionSummary", summary.toJson());
        var lookup = context.getRequestLookup();
        if ( lookup != null ) {
            result.put("issueRequestLookupCount", lookup.getLookupCount());
            result.put("issueRequestLookupLimitReached", lookup.isLimitReached());
        }
        var traceLookup = context.getTraceLookup();
        if ( traceLookup != null ) {
            result.put("issueTraceLookupCount", traceLookup.getLookupCount());
            result.put("issueTraceLookupLimitReached", traceLookup.isLimitReached());
        }
    }
}
