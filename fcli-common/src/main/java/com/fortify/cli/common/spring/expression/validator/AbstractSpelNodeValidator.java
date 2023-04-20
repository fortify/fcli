package com.fortify.cli.common.spring.expression.validator;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;

import com.fortify.cli.common.spring.expression.AbstractSpelNodeVisitor;
import com.fortify.cli.common.util.StringUtils;

/**
 * Simple abstract SpEL AST tree validator that visits the full SpEL tree until
 * the abstract {@link #getValidationError(SpelNode)} method returns 
 * a non-empty string. If a non-empty validation error string is returned by 
 * {@link #getValidationError(SpelNode)}, the abstract 
 * {@link #formatValidationError(SpelNode, String)} method is
 * called to format the validation error. Then the abstract 
 * {@link #getValidationException(String)} method is called to generate a 
 * {@link RuntimeException} instance for the given formatted validation error,
 * and the generated exception is then thrown.
 * 
 * @author rsenden
 *
 */
public abstract class AbstractSpelNodeValidator extends AbstractSpelNodeVisitor {
    public AbstractSpelNodeValidator(Expression e) { super(e); } 
    
    @Override
    protected final void visit(SpelNode node) {
        String validationError = getValidationError(node);
        if ( StringUtils.isNotBlank(validationError) ) {
            throw getValidationException(formatValidationError(node, validationError));
        }
        visitChildren(node);
    }

    protected abstract String getValidationError(SpelNode node);
    
    protected abstract String formatValidationError(SpelNode node, String msg);

    protected abstract RuntimeException getValidationException(String msg);
    
}
