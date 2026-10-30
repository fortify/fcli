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

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttribution.Scope;

import lombok.RequiredArgsConstructor;

/**
 * Attributes issue status transitions to causes. All matchers and detectors are evaluated independently;
 * every applicable cause is recorded, such that issues may have multiple attributions. Which transitions
 * a cause can explain, and with which basis, is declared by {@link FoDAttributionCause}.
 */
@RequiredArgsConstructor
public final class FoDAttributionEngine {
    private static final Comparator<FoDAttribution> ORDER = Comparator
            .comparing(FoDAttribution::basis)
            .thenComparing(FoDAttribution::scope)
            .thenComparing(FoDAttribution::cause);
    private final List<IFoDScanConditionDetector> detectors;
    private final List<IFoDIssueEvidenceMatcher> matchers;

    /** Attribute all issues in the given context, adding an 'attribution' property to each issue */
    public FoDAttributionResult attribute(FoDAttributionContext context) {
        var conditions = detectors.stream().flatMap(d -> d.detect(context).stream()).toList();
        var summary = new FoDAttributionSummary();
        for ( var issue : context.getIssues() ) {
            var attributions = attribute(context, conditions, issue);
            var status = FoDAttributionStatus.of(attributions);
            issue.node().set("attribution", toJson(status, attributions));
            summary.add(issue.transition(), status, attributions);
        }
        return new FoDAttributionResult(context, conditions, summary);
    }

    private List<FoDAttribution> attribute(FoDAttributionContext context, List<FoDScanCondition> conditions, FoDAttributionIssue issue) {
        var transition = issue.transition();
        var byCause = new EnumMap<FoDAttributionCause, FoDAttribution>(FoDAttributionCause.class);
        for ( var matcher : matchers ) {
            for ( var evidence : matcher.match(context, issue) ) {
                var basis = evidence.cause().getEffects(FoDAttributionDirection.ANY).get(transition);
                if ( basis != null ) {
                    merge(byCause, new FoDAttribution(evidence.cause(), basis, Scope.ISSUE, evidence.evidence()));
                }
            }
        }
        for ( var condition : conditions ) {
            var basis = condition.getEffects().get(transition);
            if ( basis != null ) {
                // Scan-level conditions are never issue-specific, so never provide direct evidence
                merge(byCause, new FoDAttribution(condition.cause(), basis.cap(FoDAttributionBasis.INFERRED), Scope.SCAN, null));
            }
        }
        return byCause.values().stream().sorted(ORDER).toList();
    }

    private static void merge(EnumMap<FoDAttributionCause, FoDAttribution> byCause, FoDAttribution attribution) {
        byCause.merge(attribution.cause(), attribution,
                (existing, added) -> added.basis().isStrongerThan(existing.basis()) ? added : existing);
    }

    private static ObjectNode toJson(FoDAttributionStatus status, List<FoDAttribution> attributions) {
        var result = JsonHelper.getObjectMapper().createObjectNode().put("status", status.name());
        var array = result.putArray("attributions");
        attributions.stream().map(FoDAttribution::toJson).forEach(array::add);
        return result;
    }
}
