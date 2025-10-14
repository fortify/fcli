package com.fortify.cli.common.json.record.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.record.IRecordConsumer;
import com.fortify.cli.common.json.record.IRecordProducer;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.output.transform.pipeline.JsonNodePipelineStage;

/**
 * Base {@link IRecordProducer} that applies input & record transformations defined
 * on a {@link StandardOutputConfig}. Concrete subclasses supply one or more raw
 * input {@link JsonNode} objects (for example a single JsonNode or multiple paged
 * request responses) by invoking {@link #produceRawInputs(RawInputConsumer)}.
 */
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Abstract base for building record producers that apply configured input & record transformations.
 * Subclasses use Lombok-generated builders for easier manual construction.
 */
@SuperBuilder
public abstract class AbstractTransformingRecordProducer implements IRecordProducer {
    @Getter protected final StandardOutputConfig outputConfig;
    private static final ObjectMapper OM = new ObjectMapper();
    
    private volatile boolean stopRequested; // default false
    @Override
    public final void forEach(IRecordConsumer consumer) { produceRawInputs(raw -> { if(!stopRequested){ processRawInput(raw, consumer, context(false, currentPageIndex++)); } }); }
    private long currentPageIndex; // default 0
    
    protected abstract void produceRawInputs(RawInputConsumer consumer);
    
    private void processRawInput(JsonNode rawInput, IRecordConsumer consumer, PipelineContext baseCtx) {
        JsonNode transformed = outputConfig.applyInputTransformations(rawInput);
        transformed = applyStages(outputConfig.inputStages(), transformed, baseCtx);
        if ( stopRequested || transformed==null ) { return; }
        if ( transformed.isArray() ) {
            var it = transformed.elements();
            long recordIndex = 0;
            while ( it.hasNext() && !stopRequested ) {
                if ( processPotentialRecord(it.next(), consumer, context(true, baseCtx.pageIndex(), recordIndex++))==Break.TRUE ) { return; }
            }
        } else if ( transformed.isObject() ) {
            processPotentialRecord(transformed, consumer, context(true, baseCtx.pageIndex(), 0));
        } else {
            throw new FcliBugException("Unsupported node type: "+transformed.getNodeType());
        }
    }
    
    private Break processPotentialRecord(JsonNode node, IRecordConsumer consumer, PipelineContext ctx) {
        if ( node==null ) { return Break.FALSE; }
        JsonNode record = outputConfig.applyRecordTransformations(node);
        if ( record==null ) { return Break.FALSE; }
        record = applyStages(outputConfig.recordStages(), record, ctx);
        if ( stopRequested || record==null ) { return Break.FALSE; }
        JsonNodeType type = record.getNodeType();
        ObjectNode objectToWrite = null;
        switch ( type ) {
            case ARRAY:
                if ( record.size()>0 ) { objectToWrite = (ObjectNode)parseObject(record.get(0).toString()); }
                break;
            case OBJECT:
                objectToWrite = (ObjectNode) record; break;
            case NULL: case MISSING: break;
            default: throw new FcliBugException("Invalid record node type: "+type);
        }
        return objectToWrite==null ? Break.FALSE : consumer.accept(objectToWrite);
    }

    private JsonNode applyStages(Iterable<JsonNodePipelineStage> stages, JsonNode start, PipelineContext ctx) {
        JsonNode current = start;
        for ( var stage : stages ) {
            if ( current==null || stopRequested ) { return null; }
            var outcome = stage.apply(ctx, current);
            switch ( outcome.decision() ) {
                case STOP: stopRequested = true; return null;
                case SKIP: return null;
                case CONTINUE: current = outcome.node(); break;
            }
        }
        return current;
    }
    
    private JsonNode parseObject(String s) {
        try { return OM.readTree(s); } catch ( Exception e ) { throw new FcliBugException("Error parsing record", e); }
    }

    private PipelineContext context(boolean recordPhase, long pageIndex) { return new PipelineContext(recordPhase, pageIndex, -1); }
    private PipelineContext context(boolean recordPhase, long pageIndex, long recordIndex) { return new PipelineContext(recordPhase, pageIndex, recordIndex); }
    private class PipelineContext implements JsonNodePipelineStage.TransformContext {
        private final boolean recordPhase; private final long pageIdx; private final long recordIdx;
        PipelineContext(boolean recordPhase, long pageIdx, long recordIdx){ this.recordPhase=recordPhase; this.pageIdx=pageIdx; this.recordIdx=recordIdx; }
        @Override public boolean isRecordPhase(){ return recordPhase; }
        @Override public long pageIndex(){ return pageIdx; }
        @Override public long recordIndexOnPage(){ return recordIdx; }
        @Override public Object command(){ return null; }
    }
    
    @FunctionalInterface
    protected interface RawInputConsumer { void accept(JsonNode rawInput); }
}
