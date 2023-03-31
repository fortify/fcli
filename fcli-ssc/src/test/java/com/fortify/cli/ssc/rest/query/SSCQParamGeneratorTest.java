package com.fortify.cli.ssc.rest.query;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SSCQParamGeneratorTest {
    private SSCQParamGenerator generator = new SSCQParamGenerator()
            .add("plain", SSCQParamValueGenerators::plain)
            .add("wrapped", SSCQParamValueGenerators::wrapInQuotes)
            .add("nested.nested.plain", SSCQParamValueGenerators::plain)
            .add("nested.nested.wrapped", SSCQParamValueGenerators::wrapInQuotes)
            .add("nested.reword.plain", "rewordPlain", SSCQParamValueGenerators::plain)
            .add("nested.reword.wrapped", "rewordWrapped", SSCQParamValueGenerators::wrapInQuotes);
    @ParameterizedTest
    @CsvFileSource(resources = "/com/fortify/cli/ssc/rest/query/qparam.csv", nullValues = "null")
    public void testQParamGenerator(String expressionString, String expectedQParam) {
        try {
            Expression expression = new SpelExpressionParser().parseExpression(expressionString);
            String qParam = generator.getServerSideQueryParamValue(expression);
            System.out.println(String.format("INFO: expr: %s, qParam: %s, expected: %s",expressionString, qParam, expectedQParam));
            Assertions.assertEquals(qParam, expectedQParam);
        } catch ( SpelParseException e ) {
            System.err.println(e);
            Assertions.fail(e);
        }
    }
}
