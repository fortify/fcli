package com.fortify.cli.common.output.cli.cmd.unirest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.session.manager.api.ISessionData;

import kong.unirest.UnirestInstance;

public interface IUnirestWithSessionDataJsonNodeSupplier<D extends ISessionData> {
    JsonNode getJsonNode(UnirestInstance unirest, D sessionData);
}
