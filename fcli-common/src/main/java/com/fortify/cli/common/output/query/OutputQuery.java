package com.fortify.cli.common.output.query;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public final class OutputQuery {
    private final String propertyPath;
    private final OutputQueryOperator operator;
    private final String valueToMatch;
    
    public final boolean matches(JsonNode node) {
        return operator.matches(node, propertyPath, valueToMatch);
    }
}