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
package com.fortify.cli.common.json.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.producer.pipeline.JsonNodePipelineStage;
import com.fortify.cli.common.output.processing.IRecordProducerConfig;
import com.fortify.cli.common.util.Break;

/**
 * Encapsulates transformation & pipeline logic formerly implemented by the now
 * deprecated {@code AbstractTransformingRecordProducer}. Instances are stateful with
 * respect to page index progression, so they should not be shared across different
 * logical producer instances.
 */
final class TransformationPipelineRunner {
    private static final ObjectMapper OM = new ObjectMapper();
    private final IRecordProducerConfig cfg;
    private boolean stopRequested; // default false
    private long currentPageIndex; // default 0
    TransformationPipelineRunner(IRecordProducerConfig cfg) { this.cfg = cfg; }

    public void process(JsonNode rawInput, IRecordConsumer consumer) {
        if (stopRequested) { return; }
        processRawInput(rawInput, consumer, context(false, currentPageIndex++));
    }

    private void processRawInput(JsonNode rawInput, IRecordConsumer consumer, PipelineContext baseCtx) {
        JsonNode transformed = cfg.applyInputTransformations(rawInput);
        transformed = applyStages(cfg.inputStages(), transformed, baseCtx);
        if (stopRequested || transformed == null) { return; }
        if (transformed.isArray()) {
            var it = transformed.elements();
            long recordIndex = 0;
            while (it.hasNext() && !stopRequested) {
                if (processPotentialRecord(it.next(), consumer, context(true, baseCtx.pageIndex(), recordIndex++)) == Break.TRUE) { return; }
            }
        } else if (transformed.isObject()) {
            processPotentialRecord(transformed, consumer, context(true, baseCtx.pageIndex(), 0));
        } else {
            throw new FcliBugException("Unsupported node type: "+transformed.getNodeType());
        }
    }

    private Break processPotentialRecord(JsonNode node, IRecordConsumer consumer, PipelineContext ctx) {
        if (node == null) { return Break.FALSE; }
        JsonNode record = cfg.applyRecordTransformations(node);
        if (record == null) { return Break.FALSE; }
        record = applyStages(cfg.recordStages(), record, ctx);
        if (stopRequested || record == null) { return Break.FALSE; }
        JsonNodeType type = record.getNodeType();
        ObjectNode objectToWrite = null;
        switch (type) {
            case ARRAY -> { if (record.size()>0) { objectToWrite = (ObjectNode) parseObject(record.get(0).toString()); } }
            case OBJECT -> objectToWrite = (ObjectNode) record;
            case NULL, MISSING -> { /* ignore */ }
            default -> throw new FcliBugException("Invalid record node type: "+type);
        }
        return objectToWrite==null ? Break.FALSE : consumer.accept(objectToWrite);
    }

    private JsonNode applyStages(Iterable<JsonNodePipelineStage> stages, JsonNode start, PipelineContext ctx) {
        JsonNode current = start;
        for (var stage : stages) {
            if (current==null || stopRequested) { return null; }
            var outcome = stage.apply(ctx, current);
            switch (outcome.decision()) {
                case STOP -> { stopRequested = true; return null; }
                case SKIP -> { return null; }
                case CONTINUE -> current = outcome.node();
            }
        }
        return current;
    }

    private JsonNode parseObject(String s) {
        try { return OM.readTree(s); } catch (Exception e) { throw new FcliBugException("Error parsing record", e); }
    }

    private PipelineContext context(boolean recordPhase, long pageIndex) { return new PipelineContext(recordPhase, pageIndex, -1); }
    private PipelineContext context(boolean recordPhase, long pageIndex, long recordIndex) { return new PipelineContext(recordPhase, pageIndex, recordIndex); }

    private static final class PipelineContext implements JsonNodePipelineStage.TransformContext {
        private final boolean recordPhase; private final long pageIdx; private final long recordIdx;
        PipelineContext(boolean recordPhase, long pageIdx, long recordIdx) { this.recordPhase=recordPhase; this.pageIdx=pageIdx; this.recordIdx=recordIdx; }
        public boolean isRecordPhase() { return recordPhase; }
        public long pageIndex() { return pageIdx; }
        public long recordIndexOnPage() { return recordIdx; }
        public Object command() { return null; }
    }
}
