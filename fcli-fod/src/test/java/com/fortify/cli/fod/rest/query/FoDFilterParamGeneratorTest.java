package com.fortify.cli.fod.rest.query;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;

public class FoDFilterParamGeneratorTest {
    private IServerSideQueryParamValueGenerator generator = new FoDFiltersParamGenerator()
            .add("prop")
            .add("nested.nested.prop")
            .add("nested.reword.prop", "rewordProp");
    
    @ParameterizedTest
    @CsvFileSource(resources = "/com/fortify/cli/fod/rest/query/filterparam.csv", nullValues = "null")
    public void testQParamGenerator(String expressionString, String expectedFilterParam) {
        try {
            Expression expression = new SpelExpressionParser().parseExpression(expressionString);
            String filterParam = generator.getServerSideQueryParamValue(expression);
            System.out.println(String.format("INFO: expr: %s, filterParam: %s, expected: %s",expressionString, filterParam, expectedFilterParam));
            Assertions.assertEquals(filterParam, expectedFilterParam);
        } catch ( SpelParseException e ) {
            System.err.println(e);
            Assertions.fail(e);
        }
    }
}
