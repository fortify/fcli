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

/**
 * Detects significant metric changes, based on the configured change threshold. Each rule
 * evaluates a single metric, or the ratio between two metrics, like failed requests per crawl
 * session, to avoid reporting changes that are merely caused by different scan sizes.
 */
public final class FoDAttributionMetricDetector implements IFoDScanConditionDetector {
    /** Minimum absolute change of event counters, to avoid noise from small counts */
    static final double MIN_COUNTER_CHANGE = 5;
    private final FoDAttributionCause cause;
    private final List<MetricRule> rules;

    public FoDAttributionMetricDetector(FoDAttributionCause cause, MetricRule... rules) {
        this.cause = cause;
        this.rules = List.of(rules);
    }

    /**
     * @param name Metric name
     * @param denominator Optional metric name to divide by
     * @param higherIsBroader Whether a higher value indicates a broader scan rather than degradation
     * @param minAbsoluteChange Minimum absolute change of the (numerator) metric
     */
    public record MetricRule(String name, String denominator, boolean higherIsBroader, double minAbsoluteChange) {
        /** Size metric, like file count or URL count, for which higher values indicate a broader scan */
        public static MetricRule size(String name) {
            return new MetricRule(name, null, true, 0);
        }

        /** Event counter, like crawl sessions, for which higher values indicate a broader scan */
        public static MetricRule counter(String name) {
            return new MetricRule(name, null, true, MIN_COUNTER_CHANGE);
        }

        /** Event counter relative to another metric, for which higher values indicate degradation */
        public static MetricRule degradationRatio(String name, String denominator) {
            return new MetricRule(name, denominator, false, MIN_COUNTER_CHANGE);
        }

        /** Event counter, for which higher values indicate degradation */
        public static MetricRule degradationCounter(String name) {
            return new MetricRule(name, null, false, MIN_COUNTER_CHANGE);
        }
    }

    @Override
    public List<FoDScanCondition> detect(FoDAttributionContext context) {
        var directions = new ArrayList<FoDAttributionDirection>();
        var evidence = new ArrayList<String>();
        for ( var rule : rules ) {
            var numerator = context.getMetric(rule.name());
            if ( numerator == null || !numerator.isAvailable() || numerator.getAbsoluteChange() < rule.minAbsoluteChange() ) { continue; }
            var metric = rule.denominator() == null ? numerator : ratio(numerator, context.getMetric(rule.denominator()));
            var direction = metric == null ? null : metric.getDirection(context.getChangeThresholdPercent());
            if ( direction == null ) { continue; }
            directions.add(rule.higherIsBroader() ? direction : direction.invert());
            evidence.add(describe(rule, numerator, context.getMetric(rule.denominator())));
        }
        return directions.isEmpty()
                ? List.of()
                : List.of(new FoDScanCondition(cause, FoDAttributionDirection.combine(directions), evidence));
    }

    private static FoDAttributionMetric ratio(FoDAttributionMetric numerator, FoDAttributionMetric denominator) {
        if ( denominator == null || !denominator.isAvailable() || denominator.previous() <= 0 || denominator.current() <= 0 ) {
            return null;
        }
        return new FoDAttributionMetric(numerator.previous() / denominator.previous(), numerator.current() / denominator.current());
    }

    private static String describe(MetricRule rule, FoDAttributionMetric numerator, FoDAttributionMetric denominator) {
        var result = numerator.describe(rule.name());
        return rule.denominator() == null ? result : result + String.format(" (per %s)", denominator.describe(rule.denominator()));
    }
}
