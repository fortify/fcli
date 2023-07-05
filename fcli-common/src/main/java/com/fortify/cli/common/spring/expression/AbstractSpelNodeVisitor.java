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
package com.fortify.cli.common.spring.expression;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.standard.SpelExpression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractSpelNodeVisitor {
    @Getter private final Expression rootExpression;
    
    public final String getRootExpressionString() {
        return rootExpression.getExpressionString();
    }
    
    public final void visit() {
        visit(rootExpression);
    }
    
    protected void visit(Expression expression) {
        if ( expression instanceof SpelExpression ) {
            var spelExpression = (SpelExpression)expression;
            visit(spelExpression.getAST());
        } else if ( expression instanceof LiteralExpression ) {
            var value = '"'+((LiteralExpression)expression).getValue()+'"';
            visit(new StringLiteral(value, 0, value.length(), value));
        } else {
            throw new RuntimeException("Expression type not supported: "+expression.getClass().getSimpleName()); 
        }
    }

    protected abstract void visit(SpelNode node);
    
    protected final void visitChildren(SpelNode node) {
        for ( int i=0 ; i < node.getChildCount() ; i++ ) {
            visit(node.getChild(i));
        }
    }
}
