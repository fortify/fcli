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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.ICommandHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;

/**
 * Tests for {@link ObjectNodeProducerApplyFrom} behavior.
 */
public class AbstractObjectNodeProducerApplyFromTest {
    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    @Test
    void testNoneAppliesNothing() {
    var producer = SimpleObjectNodeProducer.builder().source(F.objectNode())
        .commandHelper(dummyCommandHelper())
    .applyAllFrom(ObjectNodeProducerApplyFrom.NONE)
        .build();
    var captured = captureSingleRecord(producer);
    int fieldCount = 0; var it = captured.fieldNames(); while ( it.hasNext() ) { it.next(); fieldCount++; }
    assertEquals(0, fieldCount, "No fields expected when ApplyFrom.NONE used");
    }

    @Test
    void testProductOnlyAppliesProductTransformers() {
    var productHelper = new DummyProductHelper();
    var producer = SimpleObjectNodeProducer.builder().source(F.objectNode())
        .productHelper(productHelper)
        .commandHelper(dummyCommandHelperWithObjects(productHelper, new DummyInputTransformer(), new DummyRecordTransformer()))
    .applyAllFrom(ObjectNodeProducerApplyFrom.PRODUCT)
        .build();
        var captured = captureSingleRecord(producer);
        // Product helper provides input/record transformers via dummy implementation; user objects should be ignored
        assertTrue(captured.has("inputAdded"), "Input transformer from product helper should apply");
        assertTrue(captured.has("recordAdded"), "Record transformer from product helper should apply");
        assertFalse(captured.has("userInputAdded"), "User transformer should not be applied for PRODUCT scope");
    }

    @Test
    void testSpecAppliesAllIncludingUserObjects() {
    var productHelper = new DummyProductHelper();
    var producer = SimpleObjectNodeProducer.builder().source(F.objectNode())
        .productHelper(productHelper)
        .commandHelper(dummyCommandHelperWithObjects(productHelper, new DummyInputTransformer(), new DummyRecordTransformer(), new DummyUserInputTransformer(), new DummyUserRecordTransformer()))
    .applyAllFrom(ObjectNodeProducerApplyFrom.SPEC)
        .build();
        var captured = captureSingleRecord(producer);
        assertTrue(captured.has("inputAdded"));
        assertTrue(captured.has("recordAdded"));
        assertTrue(captured.has("userInputAdded"));
        assertTrue(captured.has("userRecordAdded"));
    }

    // --- Helpers ---
    private ObjectNode captureSingleRecord(AbstractObjectNodeProducer producer) {
        AtomicReference<ObjectNode> ref = new AtomicReference<>();
        producer.forEach(n -> { ref.set(n); return null; });
        return ref.get();
    }

    private ICommandHelper dummyCommandHelper() {
        return new ICommandHelper() {
            @Override public picocli.CommandLine.Model.CommandSpec getCommandSpec() { return picocli.CommandLine.Model.CommandSpec.create(); }
            @Override public Object getCommand() { return new Object(); }
            @Override public <T> java.util.Optional<T> getCommandAs(Class<T> type) { return java.util.Optional.empty(); }
        };
    }

    private ICommandHelper dummyCommandHelperWithObjects(Object... objects) {
        return new ICommandHelper() {
            @Override public picocli.CommandLine.Model.CommandSpec getCommandSpec() { 
                var spec = picocli.CommandLine.Model.CommandSpec.create();
                for ( var o : objects ) { spec.addMixin("m"+o.hashCode(), picocli.CommandLine.Model.CommandSpec.forAnnotatedObject(o)); }
                return spec;
            }
            @Override public Object getCommand() { return new Object(); }
            @SuppressWarnings("unchecked")
            @Override public <T> java.util.Optional<T> getCommandAs(Class<T> type) { 
                for ( var o : objects ) { if ( type.isInstance(o) ) { return java.util.Optional.of((T)o); } }
                return java.util.Optional.empty();
            }
        };
    }

    @picocli.CommandLine.Command(name="dummy")
    private static class DummyProductHelper implements IProductHelper, IInputTransformer, IRecordTransformer {
        @Override public com.fasterxml.jackson.databind.JsonNode transformInput(com.fasterxml.jackson.databind.JsonNode input) { ((ObjectNode)input).put("inputAdded", true); return input; }
        @Override public com.fasterxml.jackson.databind.JsonNode transformRecord(com.fasterxml.jackson.databind.JsonNode record) { ((ObjectNode)record).put("recordAdded", true); return record; }
    }
    @picocli.CommandLine.Command(name="dummyI")
    private static class DummyInputTransformer implements IInputTransformer { @Override public com.fasterxml.jackson.databind.JsonNode transformInput(com.fasterxml.jackson.databind.JsonNode input) { ((ObjectNode)input).put("userInputAdded", true); return input; } }
    @picocli.CommandLine.Command(name="dummyR")
    private static class DummyRecordTransformer implements IRecordTransformer { @Override public com.fasterxml.jackson.databind.JsonNode transformRecord(com.fasterxml.jackson.databind.JsonNode record) { ((ObjectNode)record).put("userRecordAdded", true); return record; } }
    private static class DummyUserInputTransformer extends DummyInputTransformer {}
    private static class DummyUserRecordTransformer extends DummyRecordTransformer {}

}
