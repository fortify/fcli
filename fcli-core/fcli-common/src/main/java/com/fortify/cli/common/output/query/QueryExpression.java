/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.query;

import org.springframework.expression.Expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.EvaluationContextFactory.EvaluationContextType;
import com.fortify.cli.common.json.JsonHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @ToString
public class QueryExpression {
    @Getter private final Expression expression;

    public boolean matches(JsonNode record) {
        try {
            return JsonHelper.evaluateSpelExpression(EvaluationContextType.USEREXPRESSIONS, record, expression, Boolean.class);
        } catch ( Exception e ) {
            throw new IllegalStateException(String.format("Error evaluating query expression:\n\tMessage: %s\n\tExpression: %s\n\tRecord: %s", e.getMessage(), expression.getExpressionString(), record.toPrettyString().replace("\n", "\n\t\t")), e);
        }
    }
}
