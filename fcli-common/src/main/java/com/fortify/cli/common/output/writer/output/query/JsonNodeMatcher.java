package com.fortify.cli.common.output.writer.output.query;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface JsonNodeMatcher {
    public boolean matches(JsonNode input, String propertyPath, String valueToMatch);
}
