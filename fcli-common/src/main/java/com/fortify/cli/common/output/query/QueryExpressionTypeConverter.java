package com.fortify.cli.common.output.query;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fortify.cli.common.spring.expression.validator.AbstractSimpleSpelNodeValidator;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class QueryExpressionTypeConverter implements ITypeConverter<QueryExpression> {
    private static final SpelExpressionParser parser = new SpelExpressionParser();
    
    @Override
    public QueryExpression convert(String value) throws Exception {
        Expression expression = null;
        try {
            expression = parser.parseExpression(value);
        } catch ( Exception e ) {
            throw new TypeConversionException(String.format("Expression cannot be parsed; please check expression syntax\n\tMessage: %s\n\tSource: %s", e.getMessage(), value));
        }
        new QueryExpressionValidator(expression).visit();
        return new QueryExpression(expression); 
    }
    
    private static final class QueryExpressionValidator extends AbstractSimpleSpelNodeValidator {
        public QueryExpressionValidator(Expression e) { super(e); }
        
        @Override
        protected String formatValidationError(SpelNode node, String msg) {
            return String.format("%s\n\tExpression: %s\n\tNode: %s", msg, getRootExpressionString(), node.toStringAST());
        }
        @Override
        protected RuntimeException getValidationException(String msg) {
            return new TypeConversionException(msg);
        }            
    }

}
