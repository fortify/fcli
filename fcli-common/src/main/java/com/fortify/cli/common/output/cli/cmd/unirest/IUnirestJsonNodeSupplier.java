package com.fortify.cli.common.output.cli.cmd.unirest;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.UnirestInstance;

public interface IUnirestJsonNodeSupplier {
    JsonNode getJsonNode(UnirestInstance unirest);
}
