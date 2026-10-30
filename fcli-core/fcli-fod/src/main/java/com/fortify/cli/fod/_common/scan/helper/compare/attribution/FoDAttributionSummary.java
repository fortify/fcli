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

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/** Per-transition counts of issue attribution statuses and attributed causes */
final class FoDAttributionSummary {
    private final Map<FoDAttributionTransition, TransitionSummary> summaries = new EnumMap<>(FoDAttributionTransition.class);

    FoDAttributionSummary() {
        for ( var transition : FoDAttributionTransition.values() ) { summaries.put(transition, new TransitionSummary()); }
    }

    void add(FoDAttributionTransition transition, FoDAttributionStatus status, Collection<FoDAttribution> attributions) {
        var summary = summaries.get(transition);
        summary.count++;
        summary.statuses.merge(status, 1, Integer::sum);
        attributions.forEach(a -> summary.causes.merge(a.cause(), 1, Integer::sum));
    }

    ObjectNode toJson() {
        var result = JsonHelper.getObjectMapper().createObjectNode();
        summaries.forEach((transition, summary) -> result.set(transition.getJsonName(), summary.toJson()));
        return result;
    }

    private static final class TransitionSummary {
        private int count;
        private final Map<FoDAttributionStatus, Integer> statuses = new EnumMap<>(FoDAttributionStatus.class);
        private final Map<FoDAttributionCause, Integer> causes = new EnumMap<>(FoDAttributionCause.class);

        private ObjectNode toJson() {
            var result = JsonHelper.getObjectMapper().createObjectNode().put("count", count);
            var statusNode = result.putObject("statuses");
            for ( var status : FoDAttributionStatus.values() ) {
                statusNode.put(status.name(), statuses.getOrDefault(status, 0));
            }
            var causeNode = result.putObject("causes");
            causes.forEach((cause, n) -> causeNode.put(cause.name(), n));
            return result;
        }
    }
}
