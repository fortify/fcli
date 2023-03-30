package com.fortify.cli.ssc.rest.query;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpEQ;

import com.fortify.cli.common.output.query.QueryExpression;
import com.fortify.cli.common.spring.expression.AbstractSpelTreeVisitor;
import com.fortify.cli.common.spring.expression.SpelNodeHelper;

public final class SSCQParamGenerator {
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
    
    public final String getQParamValue(QueryExpression queryExpression) {
        return queryExpression==null ? null : getQParamValue(queryExpression.getExpression());
    }

    public final String getQParamValue(Expression expression) {
        return new SSCQParamSpELTreeVisitor(expression).getQParamValue();
    }
    
    private final class SSCQParamSpELTreeVisitor extends AbstractSpelTreeVisitor {
        private StringBuffer qParamValue = new StringBuffer();
        
        public SSCQParamSpELTreeVisitor(Expression expression) {
            process(expression);
        }

        public String getQParamValue() {
            return qParamValue.isEmpty() ? null : qParamValue.toString();
        }
        
        @Override
        protected void visit(SpelNode node) {
            if ( node instanceof OpAnd ) {
                LOG.trace("Processing OpAnd children: {}", node.toStringAST());
                visitChildren(node);
            } else if ( node instanceof OpEQ ) {
                LOG.trace("Processing OpEQ: {}", node.toStringAST());
                visitEQ((OpEQ)node);
            }
        }

        private void visitEQ(OpEQ node) {
            var left = node.getLeftOperand();
            var right = node.getRightOperand();
            var literal = SpelNodeHelper.getFirstLiteral(right,left);
            var propertyName = SpelNodeHelper.getFirstQualifiedPropertyName(right,left);
            String literalString = literal==null ? null : literal.getLiteralValue().getValue().toString();
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
