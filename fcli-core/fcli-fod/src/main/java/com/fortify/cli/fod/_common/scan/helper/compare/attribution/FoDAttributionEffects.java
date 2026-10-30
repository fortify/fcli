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

import static com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionBasis.INFERRED;
import static com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionBasis.POSSIBLE;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Immutable mapping from issue status transition to the attribution basis with which a cause
 * can explain that transition; transitions without a mapping can't be explained by the cause.
 */
public final class FoDAttributionEffects {
    private static final FoDAttributionEffects NONE = new FoDAttributionEffects(new EnumMap<>(FoDAttributionTransition.class));
    private final Map<FoDAttributionTransition, FoDAttributionBasis> effects;

    private FoDAttributionEffects(EnumMap<FoDAttributionTransition, FoDAttributionBasis> effects) {
        this.effects = Collections.unmodifiableMap(effects);
    }

    public static FoDAttributionEffects none() {
        return NONE;
    }

    public static FoDAttributionEffects all(FoDAttributionBasis basis) {
        return NONE.newIssues(basis).reopened(basis).fixed(basis);
    }

    public FoDAttributionEffects newIssues(FoDAttributionBasis basis) {
        return with(FoDAttributionTransition.NEW, basis);
    }

    public FoDAttributionEffects reopened(FoDAttributionBasis basis) {
        return with(FoDAttributionTransition.REOPENED, basis);
    }

    public FoDAttributionEffects fixed(FoDAttributionBasis basis) {
        return with(FoDAttributionTransition.FIXED, basis);
    }

    /** @return Basis for the given transition, or null if this cause can't explain the given transition */
    public FoDAttributionBasis get(FoDAttributionTransition transition) {
        return effects.get(transition);
    }

    public boolean isEmpty() {
        return effects.isEmpty();
    }

    public ObjectNode toJson() {
        var result = JsonHelper.getObjectMapper().createObjectNode();
        effects.forEach((t, b) -> result.put(t.getJsonName(), b.name()));
        return result;
    }

    private FoDAttributionEffects with(FoDAttributionTransition transition, FoDAttributionBasis basis) {
        var copy = new EnumMap<FoDAttributionTransition, FoDAttributionBasis>(FoDAttributionTransition.class);
        copy.putAll(effects);
        copy.put(transition, basis);
        return new FoDAttributionEffects(copy);
    }

    /** Effects per scan condition direction */
    public static final class Table {
        private final Map<FoDAttributionDirection, FoDAttributionEffects> table;

        private Table(Map<FoDAttributionDirection, FoDAttributionEffects> table) {
            this.table = table;
        }

        /** Effects for non-directional causes, like issue-level evidence */
        public static Table any(FoDAttributionEffects effects) {
            return new Table(Map.of(FoDAttributionDirection.ANY, effects));
        }

        /**
         * Narrowing or degradation explains fixed issues; broadening or recovery from degradation in the previous
         * scan explains new issues, and reopened issues as the scan may have returned to the broader or better
         * state of older scans. Changes with unknown direction explain any status change.
         */
        public static Table directional() {
            return new Table(Map.of(
                    FoDAttributionDirection.DECREASE, NONE.fixed(INFERRED),
                    FoDAttributionDirection.INCREASE, NONE.newIssues(INFERRED).reopened(INFERRED),
                    FoDAttributionDirection.CHANGED, all(INFERRED)));
        }

        /** Like {@link #directional()}, but only possibly explaining any transition */
        public static Table weakDirectional() {
            return new Table(Map.of(
                    FoDAttributionDirection.DECREASE, NONE.fixed(POSSIBLE),
                    FoDAttributionDirection.INCREASE, NONE.newIssues(POSSIBLE).reopened(POSSIBLE),
                    FoDAttributionDirection.CHANGED, all(POSSIBLE)));
        }

        public FoDAttributionEffects get(FoDAttributionDirection direction) {
            return table.getOrDefault(direction, NONE);
        }
    }
}
