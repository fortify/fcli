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
/*
 * Copyright 2021-2025 Open Text.
 */
package com.fortify.cli.common.spel.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliBugException;

public class QueryExpressionCompositeTest {
    private final ObjectMapper om = new ObjectMapper();
    private final SpelExpressionParser parser = new SpelExpressionParser();
    
    private ObjectNode record(String json) throws Exception { return (ObjectNode)om.readTree(json); }
    private QueryExpression qe(String spel) { return new QueryExpression(parser.parseExpression(spel)); }
    
    @Test
    public void testAndCompositeAllTrue() throws Exception {
        var node = record("{\"a\":1, \"b\":2, \"c\":3}");
        var c = QueryExpressionComposite.and(List.of(qe("a==1"), qe("b==2"), qe("c==3")));
        assertTrue(c.matches(node));
    }
    
    @Test
    public void testAndCompositeOneFalse() throws Exception {
        var node = record("{\"a\":1, \"b\":5, \"c\":3}");
        var c = QueryExpressionComposite.and(List.of(qe("a==1"), qe("b==2"), qe("c==3")));
        assertFalse(c.matches(node));
    }
    
    @Test
    public void testCompositeRequiresMoreThanOneExpression() {
        var e = assertThrows(FcliBugException.class, () -> QueryExpressionComposite.and(List.of(qe("a==1"))));
        assertTrue(e.getMessage().contains("composite requires >1"));
    }
    
    @Test
    public void testCompositeRequiresAtLeastOneExpression() {
        var e = assertThrows(FcliBugException.class, () -> QueryExpressionComposite.and(List.of()));
        assertTrue(e.getMessage().contains("at least one"));
    }
}
