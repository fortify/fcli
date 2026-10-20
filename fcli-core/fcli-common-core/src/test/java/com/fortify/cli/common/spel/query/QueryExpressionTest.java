/*
 * Copyright 2021-2026 Open Text.
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
/*
 * Copyright 2021-2025 Open Text.
 */
package com.fortify.cli.common.spel.query;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.spel.FcliSpelExpressionParser;
import com.fortify.cli.common.spel.SpelEvaluator;

public class QueryExpressionTest {
    private final ObjectMapper om = new ObjectMapper();
    private final FcliSpelExpressionParser parser = new FcliSpelExpressionParser();
    
    private ObjectNode record(String json) throws Exception { return (ObjectNode)om.readTree(json); }
    private QueryExpression qe(String spel) { return new QueryExpression(parser.parseExpression(spel)); }
    
    @Test
    public void testSimpleMatchTrue() throws Exception {
        var node = record("{\"a\":1, \"b\":2}");
        assertTrue(qe("a==1").matches(node));
    }
    
    @Test
    public void testSimpleMatchFalse() throws Exception {
        var node = record("{\"a\":1, \"b\":2}");
        assertFalse(qe("a==2").matches(node));
    }
    
    @Test
    public void testMultipleConditionsTrue() throws Exception {
        var node = record("{\"a\":1, \"b\":2, \"name\":\"fortify\"}");
        assertTrue(qe("a==1 and b==2 and name=='fortify'").matches(node));
    }
    
    @Test
    public void testMissingPropertyCausesTechnicalException() throws Exception {
        var node = record("{\"a\":1}");
        var ex = assertThrows(FcliTechnicalException.class, () -> qe("b==2").matches(node));
        assertTrue(ex.getMessage().contains("Expression:"));
    }

    @Test
    public void testQueryConverterAndEvaluatorsWithLargeIds() throws Exception {
        var source = "id == 3000000000 and label == '3000000000'";
        var node = record("{\"id\":3000000000,\"label\":\"3000000000\"}");
        var query = new QueryExpressionTypeConverter().convert(source);
        assertTrue(query.matches(node));
        assertEquals(source, query.getExpression().getExpressionString());
        assertTrue(SpelEvaluator.JSON_QUERY.evaluate(source, node, Boolean.class));
        assertTrue(SpelEvaluator.JSON_GENERIC.copy().evaluate(source, node, Boolean.class));
    }
}
