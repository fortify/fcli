package com.fortify.cli.common.output.query;

import org.springframework.expression.Expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @ToString
public class QueryExpression {
    @Getter private final Expression expression;

    public boolean matches(JsonNode record) {
        try {
            return JsonHelper.evaluateSpelExpression(record, expression, Boolean.class);
        } catch ( Exception e ) {
            throw new IllegalStateException(String.format("Error evaluating query expression:\n\tMessage: %s\n\tExpression: %s\n\tRecord: %s", e.getMessage(), expression.getExpressionString(), record.toPrettyString().replace("\n", "\n\t\t")));
        }
    }
}
