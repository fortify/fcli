package com.fortify.cli.common.output.spi.transform;

import com.fasterxml.jackson.databind.JsonNode;

public interface IRecordTransformer {
    JsonNode transformRecord(JsonNode record);
}
