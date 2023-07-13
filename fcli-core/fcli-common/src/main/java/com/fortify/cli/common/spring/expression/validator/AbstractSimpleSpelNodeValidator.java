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
package com.fortify.cli.common.spring.expression.validator;

import java.util.HashMap;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.Assign;
import org.springframework.expression.spel.ast.BeanReference;
import org.springframework.expression.spel.ast.ConstructorReference;
import org.springframework.expression.spel.ast.TypeReference;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

/**
 * This {@link AbstractSpelNodeValidator} implementation validates that the SpEL 
 * tree doesn't contain any unsupported node types, as returned by the 
 * {@link #createUnsupportedNodesMap()} method. Subclasses can override this method
 * to return a different set of unsupported node types; the default implementation
 * returns node types that are commonly not supported by {@link SimpleEvaluationContext}.
 * @author rsenden
 *
 */
public abstract class AbstractSimpleSpelNodeValidator extends AbstractSpelNodeValidator { 
    private final Map<Class<? extends SpelNode>, String> unsupportedNodes = createUnsupportedNodesMap();
    
    public AbstractSimpleSpelNodeValidator(Expression e) { super(e); }
    
    @Override
    protected String getValidationError(SpelNode node) {
        return unsupportedNodes.get(node.getClass());
    }

    protected Map<Class<? extends SpelNode>, String> createUnsupportedNodesMap() {
        Map<Class<? extends SpelNode>, String> result = new HashMap<>();
        result.put(Assign.class, "Expressions containing assignments are not supported; did you mean to use '==' instead of '='?");
        result.put(BeanReference.class, "Expressions containing bean references are not supported");
        result.put(ConstructorReference.class, "Expressions containing constructor references are not supported");
        result.put(TypeReference.class, "Expressions containing type references are not supported");
        return result;
    }    
}
