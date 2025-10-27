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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fortify.cli.common.exception.FcliBugException;

import lombok.Getter;
import lombok.ToString;

/**
 * Composite {@link QueryExpression} that joins multiple child {@link QueryExpression} instances
 * using a configurable logical operator (AND/OR). The resulting composite is represented as a
 * single SpEL expression string, allowing existing code that consumes {@link QueryExpression}
 * to keep working unchanged.
 */
@ToString(callSuper = true)
public class QueryExpressionComposite extends QueryExpression {
    public enum LogicalOperator {
        AND("and"), OR("or");
        @Getter private final String spelOperator;
        LogicalOperator(String spelOperator) { this.spelOperator = spelOperator; }
    }

    @Getter private final List<QueryExpression> children;
    @Getter private final LogicalOperator operator;

    private static final SpelExpressionParser parser = new SpelExpressionParser();

    private QueryExpressionComposite(Expression expression, List<QueryExpression> children, LogicalOperator operator) {
        super(expression);
        this.children = children;
        this.operator = operator;
    }

    /**
     * Factory method creating a composite that AND's all provided expressions.
     */
    public static QueryExpressionComposite and(List<QueryExpression> expressions) {
        return create(expressions, LogicalOperator.AND);
    }

    /**
     * Factory method creating a composite that OR's all provided expressions.
     */
    public static QueryExpressionComposite or(List<QueryExpression> expressions) {
        return create(expressions, LogicalOperator.OR);
    }

    private static QueryExpressionComposite create(List<QueryExpression> expressions, LogicalOperator op) {
        if ( expressions==null || expressions.isEmpty() ) {
            throw new FcliBugException("Composite creation requires at least one expression");
        }
        if ( expressions.size()==1 ) {
            throw new FcliBugException("Use single expression directly; composite requires >1 expressions");
        }
        var combinedSource = expressions.stream()
                .map(qe -> "("+qe.getExpression().getExpressionString()+")")
                .collect(Collectors.joining(" "+op.getSpelOperator()+" "));
        Expression expression;
        try {
            expression = parser.parseExpression(combinedSource);
        } catch ( Exception e ) {
            throw new FcliBugException("Failed to parse composite query expression: "+combinedSource, e);
        }
        return new QueryExpressionComposite(expression, List.copyOf(expressions), op);
    }
}
