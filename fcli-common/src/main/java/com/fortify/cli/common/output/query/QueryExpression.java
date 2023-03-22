package com.fortify.cli.common.output.query;

import org.springframework.expression.Expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryExpression {
    @Getter private final Expression expression;

    public boolean matches(JsonNode record) {
        return JsonHelper.evaluateSpELExpression(record, expression, Boolean.class);
    }
}
