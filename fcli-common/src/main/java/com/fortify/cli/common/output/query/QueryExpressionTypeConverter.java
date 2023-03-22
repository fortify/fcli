package com.fortify.cli.common.output.query;

import org.springframework.expression.spel.standard.SpelExpressionParser;

import picocli.CommandLine.ITypeConverter;

public class QueryExpressionTypeConverter implements ITypeConverter<QueryExpression> {
    private static final SpelExpressionParser parser = new SpelExpressionParser();
    
    @Override
    public QueryExpression convert(String value) throws Exception {
        return new QueryExpression(parser.parseExpression(value)); 
    }

}
