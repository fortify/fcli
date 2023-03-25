package com.fortify.cli.common.output.transform;

import com.fasterxml.jackson.databind.JsonNode;

public interface IInputTransformer {
    JsonNode transformInput(JsonNode input);
}
