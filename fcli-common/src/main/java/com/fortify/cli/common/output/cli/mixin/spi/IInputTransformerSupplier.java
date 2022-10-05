package com.fortify.cli.common.output.cli.mixin.spi;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;

public interface IInputTransformerSupplier {
    default UnaryOperator<JsonNode> getInputTransformer() { return this::transformInput; }
    default JsonNode transformInput(JsonNode input) { return input; }
}
