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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

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
        getDrivers(status, attributions).forEach(cause -> summary.driverCauses.merge(cause, 1, Integer::sum));
    }

    ObjectNode toJson() {
        var result = JsonHelper.getObjectMapper().createObjectNode();
        summaries.forEach((transition, summary) -> result.set(transition.getJsonName(), summary.toJson()));
        return result;
    }

    /**
     * @return Number of issues per driving cause for the given transition, ordered by descending count. Driving causes
     *         are the causes that determined each issue's attribution status, like DIRECT attributions for DIRECT issues;
     *         INDETERMINATE issues don't have any driving causes.
     */
    Map<FoDAttributionCause, Integer> getDriverCauses(FoDAttributionTransition transition) {
        return sortByDescendingCount(summaries.get(transition).driverCauses);
    }

    int getStatusCount(FoDAttributionTransition transition, FoDAttributionStatus status) {
        return summaries.get(transition).statuses.getOrDefault(status, 0);
    }

    // Causes that determined the given status; consistent with FoDAttributionStatus.of()
    private static Stream<FoDAttributionCause> getDrivers(FoDAttributionStatus status, Collection<FoDAttribution> attributions) {
        return switch ( status ) {
            case DIRECT -> causesWithBasis(attributions, FoDAttributionBasis.DIRECT);
            case INFERRED -> causesWithBasis(attributions, FoDAttributionBasis.INFERRED);
            case INDETERMINATE -> Stream.empty();
        };
    }

    private static Stream<FoDAttributionCause> causesWithBasis(Collection<FoDAttribution> attributions, FoDAttributionBasis basis) {
        return attributions.stream().filter(a -> a.basis() == basis).map(FoDAttribution::cause);
    }

    private static Map<FoDAttributionCause, Integer> sortByDescendingCount(Map<FoDAttributionCause, Integer> causes) {
        var result = new LinkedHashMap<FoDAttributionCause, Integer>();
        causes.entrySet().stream()
                .sorted(Map.Entry.<FoDAttributionCause, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    private static final class TransitionSummary {
        private int count;
        private final Map<FoDAttributionStatus, Integer> statuses = new EnumMap<>(FoDAttributionStatus.class);
        private final Map<FoDAttributionCause, Integer> causes = new EnumMap<>(FoDAttributionCause.class);
        private final Map<FoDAttributionCause, Integer> driverCauses = new EnumMap<>(FoDAttributionCause.class);

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
