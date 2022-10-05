package com.fortify.cli.common.output.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.UnirestInstance;

public interface IJsonNodeSupplier {
    JsonNode getJsonNode(UnirestInstance unirest);
}
