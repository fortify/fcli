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
package com.fortify.cli.fod.rest.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.InlineList;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpEQ;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.OperatorMatches;

import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.common.spring.expression.AbstractSpelNodeVisitor;
import com.fortify.cli.common.spring.expression.SpelNodeHelper;
import com.fortify.cli.common.util.JavaHelper;

public final class FoDFiltersParamGenerator implements IServerSideQueryParamValueGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(FoDFiltersParamGenerator.class);
    // TODO Review this pattern
    // This should match any characters/sequences that have a special meaning in regex (apart from '|'), 
    // unless they have been escaped. Alternatively, we could have a regex that only matches simple
    // literal 'or' regexes (i.e. not containing any special regex characters), adjusting the logic
    // where this pattern is used.
    private static final Pattern SPECIAL_REGEX_CHAR_PATTERN = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*[\\[\\].+*?^$(){}]|(?<=\\\\)[\\d\\w]");
    private final Map<String, String> filterNamesByPropertyPaths = new HashMap<>();
    
    public FoDFiltersParamGenerator add(String propertyPath, String filterName) {
        filterNamesByPropertyPaths.put(propertyPath, filterName);
        return this;
    }

    public FoDFiltersParamGenerator add(String propertyPath) {
        return add(propertyPath, propertyPath);
    }

    @Override
    public final String getServerSideQueryParamValue(Expression expression) {
        return new FoDFilterParamSpELNodeVisitor(expression).getQParamValue();
    }
    
    private final class FoDFilterParamSpELNodeVisitor extends AbstractSpelNodeVisitor {
        private StringBuffer filterParamValue = new StringBuffer();
        
        public FoDFilterParamSpELNodeVisitor(Expression expression) {
            super(expression); visit();
        }

        public String getQParamValue() {
            return filterParamValue.isEmpty() ? null : filterParamValue.toString();
        }
        
        @Override
        protected void visit(SpelNode node) {
            LOG.trace("Visiting node: "+node);
            JavaHelper.as(node, OpAnd.class).ifPresent(this::visitAnd);
            JavaHelper.as(node, OpEQ.class).ifPresent(this::visitEQ);
            JavaHelper.as(node, OpOr.class).ifPresent(this::visitOr);
            JavaHelper.as(node, CompoundExpression.class).ifPresent(this::visitCompoundExpression);
            JavaHelper.as(node, OperatorMatches.class).ifPresent(this::visitOperatorMatches);
        }
        
        private void visitAnd(OpAnd node) {
            LOG.trace("Processing OpAnd node: "+node);
            visitChildren(node);
        }
        
        private void visitEQ(OpEQ node) {
            LOG.trace("Processing OpEq node: "+node);
            var propertyName = SpelNodeHelper.operand(node, SpelNodeHelper::qualifiedPropertyName).orElse(null);
            var literalString = SpelNodeHelper.operand(node, SpelNodeHelper::literalString).orElse(null);
            LOG.trace("OpEQ property: {}, literal: {}", propertyName, literalString);
            if ( propertyName!=null && literalString!=null ) {
                addEQ(propertyName, literalString);
            }
        }
        
        private void visitOr(OpOr node) {
            LOG.trace("Processing OpOr node: "+node);
            SpelNodeHelper.mapChildrenStream(node, OpEQ.class).ifPresent(this::collectOr);
        }
        
        private void visitOperatorMatches(OperatorMatches node) {
            LOG.trace("Processing OperatorMatches node: "+node);
            var propertyName = SpelNodeHelper.operand(node, SpelNodeHelper::qualifiedPropertyName).orElse(null);
            var literalString = SpelNodeHelper.operand(node, SpelNodeHelper::literalString).orElse(null);
            if ( propertyName!=null && literalString!=null ) {
                if ( !SPECIAL_REGEX_CHAR_PATTERN.matcher(literalString).find() ) {
                    List<String> values = Arrays.asList(literalString.replaceAll("\\\\", "").split("\\|"));
                    addOr(propertyName, values);
                }
            }
        }
        
        private void visitCompoundExpression(CompoundExpression node) {
            LOG.trace("Processing CompoundExpression node: "+node);
            if ( node.getChildCount()==2 ) {
                var inlineList = JavaHelper.as(node.getChild(0), InlineList.class);
                var methodReference = JavaHelper.as(node.getChild(1), MethodReference.class);
                inlineList.ifPresent(list->methodReference.ifPresent(mr->visitInlineListMethodReference(list, mr)));
            }
        }
        
        private void visitInlineListMethodReference(InlineList inlineList, MethodReference methodReference) {
            if ( methodReference.getName().equals("contains") && methodReference.getChildCount()==1 ) {
                var values = SpelNodeHelper.collectChildren(inlineList, Literal.class, SpelNodeHelper::literalStringValue, Collectors.toList());
                var propertyName = SpelNodeHelper.qualifiedPropertyName(methodReference.getChild(0));
                propertyName.ifPresent(p->values.ifPresent(v->addOr(p,v)));
            }
        }

        private void collectOr(Stream<OpEQ> stream) {
            // First list entry will be the property name, remaining entries the values to be matched
            List<String> results = new ArrayList<>();
            if ( stream.map(opEq->collectOr(opEq, results)).allMatch(Boolean::booleanValue) && results.size()>1 ) {
                addOr(results.get(0), results.subList(1, results.size()));
            }
        }

        private boolean collectOr(OpEQ opEq, List<String> result) {
            var propertyName = SpelNodeHelper.operand(opEq, SpelNodeHelper::qualifiedPropertyName).orElse(null);
            var literalString = SpelNodeHelper.operand(opEq, SpelNodeHelper::literalString).orElse(null);
            // We only support equals operators comparing simple property names with literals 
            if ( propertyName==null || literalString==null ) { return false; }
            // Add property name if not already added, otherwise check that
            // previously stored property name matches current property name
            // (FoD only supports OR operations on single field)
            if ( result.isEmpty() ) { result.add(propertyName); } 
            else if ( !propertyName.equals(result.get(0)) ) { return false; }
            result.add(literalString);
            return true;
        }
        
        private void addOr(String propertyName, Collection<String> values) {
            addEQ(propertyName, values.stream().collect(Collectors.joining("|")));
        }
        
        private void addEQ(String propertyName, String value) {
            String qName = filterNamesByPropertyPaths.get(propertyName);
            if ( qName!=null ) {
                addQuery(String.format("%s:%s", qName, value));
            }
        }

        private void addQuery(String query) {
            filterParamValue.append(
                filterParamValue.isEmpty()
                    ? query
                    : ("+"+query)
            );
        }
    }
}