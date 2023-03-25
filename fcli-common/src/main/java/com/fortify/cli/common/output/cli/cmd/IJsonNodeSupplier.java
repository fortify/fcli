package com.fortify.cli.common.output.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;

public interface IJsonNodeSupplier {
    JsonNode getJsonNode();
}
