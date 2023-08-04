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
package com.fortify.cli.fod._common.rest.query;

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
