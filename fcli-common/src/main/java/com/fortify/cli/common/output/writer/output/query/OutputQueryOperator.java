package com.fortify.cli.common.output.writer.output.query;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OutputQueryOperator implements JsonNodeMatcher {
    EQUALS("=", OutputQueryOperator::evaluateEquals);
    
    @Getter private final String operator;
    private final JsonNodeMatcher matcher;
    
    @Override
    public boolean matches(JsonNode input, String propertyPath, String valueToMatch) {
        return matcher.matches(input, propertyPath, valueToMatch);
    }
    
    public static final String[] operators() {
        return Stream.of(OutputQueryOperator.values()).map(OutputQueryOperator::getOperator).toArray(String[]::new);
    }
    
    public static final OutputQueryOperator valueOfOperator(String operator) {
        return Stream.of(OutputQueryOperator.values())
                .filter(o->o.getOperator().equalsIgnoreCase(operator))
                .findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unknown query operator '"+operator+"'"));
    }
    
    private static Boolean evaluateEquals(JsonNode node, String propertyPath, String valueToMatch) {
        return valueToMatch.equals(JsonHelper.evaluateJsonPath(node, propertyPath, String.class));
    }
}