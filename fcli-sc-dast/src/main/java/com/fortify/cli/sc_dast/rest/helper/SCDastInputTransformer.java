package com.fortify.cli.sc_dast.rest.helper;

import com.fasterxml.jackson.databind.JsonNode;

public class SCDastInputTransformer {
    public static final JsonNode getItems(JsonNode input) {
        if ( input.has("items") ) { return input.get("items"); }
        if ( input.has("item") ) { return input.get("item"); }
        return input;
    }
}
