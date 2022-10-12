package com.fortify.cli.common.output.cli.mixin.spi.output;

import com.fasterxml.jackson.databind.JsonNode;

public interface IJsonNodeOutputHelper extends IOutputHelperBase {
    void write(JsonNode jsonNode);
}
