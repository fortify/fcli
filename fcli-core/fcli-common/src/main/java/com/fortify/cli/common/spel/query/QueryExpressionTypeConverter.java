/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.spel.query;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spel.validator.AbstractSimpleSpelNodeValidator;

import picocli.CommandLine.ITypeConverter;

public class QueryExpressionTypeConverter implements ITypeConverter<QueryExpression> {
    private static final SpelExpressionParser parser = new SpelExpressionParser();
    
    @Override
    public QueryExpression convert(String value) throws Exception {
        Expression expression = null;
        try {
            expression = parser.parseExpression(value);
        } catch ( Exception e ) {
            throw new FcliSimpleException(String.format("Expression cannot be parsed; please check expression syntax\n\tMessage: %s\n\tSource: %s", e.getMessage(), value));
        }
        new QueryExpressionValidator(expression).visit();
        return new QueryExpression(expression); 
    }
    
    /**
     * This class checks for node types that are not supported in queries (as defined in 
     * {@link AbstractSimpleSpelNodeValidator}), throwing an appropriate exception if
     * necessary..
     */
    private static final class QueryExpressionValidator extends AbstractSimpleSpelNodeValidator {
        public QueryExpressionValidator(Expression e) { super(e); }
        
        @Override
        protected String formatValidationError(SpelNode node, String msg) {
            return String.format("%s\n\tExpression: %s\n\tNode: %s", msg, getRootExpressionString(), node.toStringAST());
        }
        @Override
        protected RuntimeException getValidationException(String msg) {
            return new FcliSimpleException(msg);
        }            
    }

}
