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
package com.fortify.cli.common.output.processing;

import com.fortify.cli.common.output.transform.pipeline.QueryFilterStage;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.spel.query.IQueryExpressionSupplier;

import picocli.CommandLine;

/**
 * Utility to attach a query filter stage based on command/mixins/writer factory
 * suppliers.
 */
public final class QueryStageConfigurator {
    private QueryStageConfigurator() {
    }
    public static void configure(StandardOutputConfig cfg, CommandLine.Model.CommandSpec commandSpec, Object command,
            Object outputWriterFactory) {
        if (cfg.recordStages().stream().anyMatch(s -> s instanceof QueryFilterStage)) {
            return;
        }
        // Scan mixins
        for (var mixin : commandSpec.mixins().values()) {
            var o = mixin.userObject();
            if (addIfSupplier(cfg, o)) {
                return;
            }
        }
        // Command
        if (addIfSupplier(cfg, command)) {
            return;
        }
        // Writer factory (may hold arg groups)
        addIfSupplier(cfg, outputWriterFactory);
    }
    private static boolean addIfSupplier(StandardOutputConfig cfg, Object o) {
        if (o instanceof IQueryExpressionSupplier s && s.getQueryExpression() != null) {
            cfg.recordStage(new QueryFilterStage(s.getQueryExpression()));
            return true;
        }
        return false;
    }
}
