package com.fortify.cli.ssc.rest.helper;

import com.fasterxml.jackson.databind.JsonNode;

public class SSCInputTransformer {
    public static final JsonNode getDataOrSelf(JsonNode json) {
        return json.has("data") ? json.get("data") : json;
    }
}
