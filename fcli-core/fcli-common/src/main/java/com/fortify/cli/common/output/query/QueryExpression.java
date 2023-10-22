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
import com.fortify.cli.common.spring.expression.SpelEvaluator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @ToString
public class QueryExpression {
    @Getter private final Expression expression;

    public boolean matches(JsonNode record) {
        try {
            return SpelEvaluator.JSON_QUERY.evaluate(expression, record, Boolean.class);
        } catch ( Exception e ) {
            var msg = e.getMessage();
            if ( msg.startsWith("EL1008E") ) {
                msg += "\n\t         Please check that the property exists. Note that literal values must be enclosed in single or double quotes."
                      +"\n\t         Potentially the quotes need to be escaped to avoid them from being removed by the shell.";
            }
            throw new IllegalStateException(String.format("Error evaluating query expression:\n\tMessage: %s\n\tExpression: %s\n\tRecord: %s", msg, expression.getExpressionString(), record.toPrettyString().replace("\n", "\n\t\t")), e);
        }
    }
}
