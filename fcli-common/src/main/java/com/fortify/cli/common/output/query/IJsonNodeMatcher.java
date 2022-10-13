package com.fortify.cli.common.output.query;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface IJsonNodeMatcher {
    public boolean matches(JsonNode input, String propertyPath, String valueToMatch);
}
