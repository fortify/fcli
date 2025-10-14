package com.fortify.cli.common.output.transform.pipeline;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unified pipeline stage for input & record processing.
 */
public interface JsonNodePipelineStage {
    TransformOutcome apply(TransformContext ctx, JsonNode node);
    enum Decision { CONTINUE, SKIP, STOP }
    record TransformOutcome(JsonNode node, Decision decision) {
        public static TransformOutcome continueWith(JsonNode n) { return new TransformOutcome(n, Decision.CONTINUE); }
        public static TransformOutcome skip() { return new TransformOutcome(null, Decision.SKIP); }
        public static TransformOutcome stop() { return new TransformOutcome(null, Decision.STOP); }
    }
    interface TransformContext {
        boolean isRecordPhase();
        long pageIndex();
        long recordIndexOnPage();
        Object command();
    }
}
