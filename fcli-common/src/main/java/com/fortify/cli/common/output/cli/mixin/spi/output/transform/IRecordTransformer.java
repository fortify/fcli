package com.fortify.cli.common.output.cli.mixin.spi.output.transform;

import com.fasterxml.jackson.databind.JsonNode;

public interface IRecordTransformer {
    JsonNode transformRecord(JsonNode record);
}
