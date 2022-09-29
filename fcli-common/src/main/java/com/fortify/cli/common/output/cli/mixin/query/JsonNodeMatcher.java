package com.fortify.cli.common.output.cli.mixin.query;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface JsonNodeMatcher {
    public boolean matches(JsonNode input, String propertyPath, String valueToMatch);
}
