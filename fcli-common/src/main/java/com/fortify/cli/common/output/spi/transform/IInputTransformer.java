package com.fortify.cli.common.output.spi.transform;

import com.fasterxml.jackson.databind.JsonNode;

public interface IInputTransformer {
    JsonNode transformInput(JsonNode input);
}
