package com.fortify.cli.common.output.cli.mixin.spi;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;

public interface IRecordTransformerSupplier {
    default UnaryOperator<JsonNode> getRecordTransformer() { return this::transformRecord; }
    default JsonNode transformRecord(JsonNode record) { return record; }
}
