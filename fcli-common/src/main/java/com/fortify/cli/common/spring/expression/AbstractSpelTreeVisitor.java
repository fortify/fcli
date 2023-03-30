package com.fortify.cli.common.spring.expression;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;

public abstract class AbstractSpELTreeVisitor {
    public final void process(Expression e) {
        if ( e instanceof SpelExpression ) {
            visit(((SpelExpression) e).getAST());
        }
    }

    protected abstract void visit(SpelNode node);
    
    protected final void visitChildren(SpelNode node) {
        for ( int i=0 ; i < node.getChildCount() ; i++ ) {
            visit(node.getChild(i));
        }
    }
}
