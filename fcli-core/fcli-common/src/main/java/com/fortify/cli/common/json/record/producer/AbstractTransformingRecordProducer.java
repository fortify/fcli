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
    
    @Override
    public final void forEach(IRecordConsumer consumer) {
        produceRawInputs(raw -> processRawInput(raw, consumer));
    }
    
    protected abstract void produceRawInputs(RawInputConsumer consumer);
    
    private void processRawInput(JsonNode rawInput, IRecordConsumer consumer) {
        JsonNode transformed = outputConfig.applyInputTransformations(rawInput);
        if ( transformed==null ) { return; }
        if ( transformed.isArray() ) {
            var it = transformed.elements();
            while ( it.hasNext() ) {
                if ( processPotentialRecord(it.next(), consumer)==Break.TRUE ) { return; }
            }
        } else if ( transformed.isObject() ) {
            processPotentialRecord(transformed, consumer);
        } else {
            throw new FcliBugException("Unsupported node type: "+transformed.getNodeType());
        }
    }
    
    private Break processPotentialRecord(JsonNode node, IRecordConsumer consumer) {
        if ( node==null ) { return Break.FALSE; }
        JsonNode record = outputConfig.applyRecordTransformations(node);
        if ( record==null ) { return Break.FALSE; }
        JsonNodeType type = record.getNodeType();
        ObjectNode objectToWrite = null;
        switch ( type ) {
        case ARRAY:
            if ( record.size()>0 ) {
                // Re-parse first element to ensure ObjectNode instance
                objectToWrite = (ObjectNode)parseObject(record.get(0).toString());
            }
            break;
        case OBJECT:
            objectToWrite = (ObjectNode) record;
            break;
        case NULL: case MISSING:
            break;
        default:
            throw new FcliBugException("Invalid record node type: "+type);
        }
        return objectToWrite==null ? Break.FALSE : consumer.accept(objectToWrite);
    }
    
    private JsonNode parseObject(String s) {
        try { return OM.readTree(s); } catch ( Exception e ) { throw new FcliBugException("Error parsing record", e); }
    }
    
    @FunctionalInterface
    protected interface RawInputConsumer { void accept(JsonNode rawInput); }
}
