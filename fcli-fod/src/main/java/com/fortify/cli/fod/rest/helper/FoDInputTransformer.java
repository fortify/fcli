package com.fortify.cli.fod.rest.helper;

import com.fasterxml.jackson.databind.JsonNode;

public class FoDInputTransformer {
    public static final JsonNode getItems(JsonNode input) {
        if ( input.has("items") ) { return input.get("items"); }
        return input;
    }
}
