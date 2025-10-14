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
package com.fortify.cli.common.output.transform.pipeline;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unified pipeline stage for input & record processing.
 */
public interface JsonNodePipelineStage {
    TransformOutcome apply(TransformContext ctx, JsonNode node);
    enum Decision {
        CONTINUE, SKIP, STOP
    }
    record TransformOutcome(JsonNode node, Decision decision) {
        public static TransformOutcome continueWith(JsonNode n) {
            return new TransformOutcome(n, Decision.CONTINUE);
        }
        public static TransformOutcome skip() {
            return new TransformOutcome(null, Decision.SKIP);
        }
        public static TransformOutcome stop() {
            return new TransformOutcome(null, Decision.STOP);
        }
    }
    interface TransformContext {
        boolean isRecordPhase();
        long pageIndex();
        long recordIndexOnPage();
        Object command();
    }
}
