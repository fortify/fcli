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
package com.fortify.cli.ssc._common.rest.query;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpEQ;

import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.common.spring.expression.AbstractSpelNodeVisitor;
import com.fortify.cli.common.spring.expression.SpelNodeHelper;
import com.fortify.cli.common.util.JavaHelper;

public final class SSCQParamGenerator implements IServerSideQueryParamValueGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SSCQParamGenerator.class);
    private final Map<String, String> qNamesByPropertyPaths = new HashMap<>();
    private final Map<String, Function<String,String>> valueGeneratorsByPropertyPaths = new HashMap<>();
    
    public SSCQParamGenerator add(String propertyPath, String qName, Function<String,String> valueGenerator) {
        qNamesByPropertyPaths.put(propertyPath, qName);
        valueGeneratorsByPropertyPaths.put(propertyPath, valueGenerator);
        return this;
    }
    
    public SSCQParamGenerator add(String propertyPath, Function<String,String> valueGenerator) {
        qNamesByPropertyPaths.put(propertyPath, propertyPath);
        valueGeneratorsByPropertyPaths.put(propertyPath, valueGenerator);
        return this;
    }
    
    @Override
    public final String getServerSideQueryParamValue(Expression expression) {
        return new SSCQParamSpELNodeVisitor(expression).getQParamValue();
    }
    
    private final class SSCQParamSpELNodeVisitor extends AbstractSpelNodeVisitor {
        private StringBuffer qParamValue = new StringBuffer();
        
        public SSCQParamSpELNodeVisitor(Expression expression) {
            super(expression); visit();
        }

        public String getQParamValue() {
            return qParamValue.isEmpty() ? null : qParamValue.toString();
        }
        
        @Override
        protected void visit(SpelNode node) {
            LOG.trace("Visiting node: "+node);
            JavaHelper.as(node, OpAnd.class).ifPresent(this::visitAnd);
            JavaHelper.as(node, OpEQ.class).ifPresent(this::visitEQ);
        }
        
        private void visitAnd(OpAnd node) {
            LOG.trace("Processing OpAnd node: "+node);
            visitChildren(node);
        }

        private void visitEQ(OpEQ node) {
            var propertyName = SpelNodeHelper.operand(node, SpelNodeHelper::qualifiedPropertyName).orElse(null);
            var literalString = SpelNodeHelper.operand(node, SpelNodeHelper::literalString).orElse(null);
            LOG.trace("OpEQ property: {}, literal: {}", propertyName, literalString);
            if ( propertyName!=null && literalString!=null ) {
                addEQ(propertyName, literalString);
            }
        }
        
        private void addEQ(String propertyName, String value) {
            String qName = qNamesByPropertyPaths.get(propertyName);
            if ( qName!=null ) {
                Function<String, String> valueGenerator = valueGeneratorsByPropertyPaths.get(propertyName);
                addQuery(String.format("%s:%s", qName, valueGenerator.apply(value)));
            }
        }

        private void addQuery(String query) {
            qParamValue.append(
                qParamValue.isEmpty()
                    ? query
                    : ("+and+"+query)
            );
        }
    }
}
