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

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;

import com.fortify.cli.common.util.JavaHelper;

import lombok.RequiredArgsConstructor;

/**
 * This class contains various utility methods for working with {@link SpelNode}
 * instances.
 * @author rsenden
 *
 */
public class SpelNodeHelper {
    private SpelNodeHelper() {}
    
    /**
     * Apply the given function to the left operand and return the result if not empty,
     * otherwise return the result of applying the given function on the right operand
     * (which may again be empty).
     */
    public static final <R> Optional<R> operand(Operator node, Function<SpelNode, Optional<R>> f) {
        if ( node==null ) { return null; }
        var left = node.getLeftOperand();
        var right = node.getRightOperand();
        return f.apply(left).or(()->f.apply(right));
    }
    
    /**
     * Return an optional literal string value if the given node represents a {@link Literal}.
     */
    public static final Optional<String> literalString(SpelNode node) {
        return JavaHelper.as(node, Literal.class).map(SpelNodeHelper::literalStringValue);
    }
    
    public static final String literalStringValue(Literal literal) {
        return literal.getLiteralValue().getValue().toString();
    }
    
    /**
     * Return an optional qualified property name if the given node represents either a
     * {@link PropertyOrFieldReference} or {@link CompoundExpression} containing
     * only {@link PropertyOrFieldReference} children.
     */
    public static final Optional<String> qualifiedPropertyName(SpelNode node) {
        return propertyOrFieldReferenceName(node)
                .or(()->SpelNodeHelper.compoundPropertyName(node));
    }
    
    /**
     * Return an optional property name if the given node represents a 
     * {@link PropertyOrFieldReference}.
     */
    public static final Optional<String> propertyOrFieldReferenceName(SpelNode node) {
        return JavaHelper.as(node, PropertyOrFieldReference.class)
                .map(PropertyOrFieldReference::getName);
    }
    
    /**
     * Return an optional compound property name if the given node represents 
     * a {@link CompoundExpression} containing only {@link PropertyOrFieldReference} 
     * children.
     */
    public static final Optional<String> compoundPropertyName(SpelNode node) {
        return JavaHelper
            .as(node, CompoundExpression.class)
            .flatMap(ceNode->
                collectChildren(ceNode, PropertyOrFieldReference.class, 
                        PropertyOrFieldReference::getName, Collectors.joining(".")));
                
    }
    
    /**
     * Return a {@link Predicate} that returns true if the value passed to
     * the predicate is of the given type.
     */
    public static final <T extends SpelNode> Predicate<SpelNode> is(Class<T> type) {
        return node->JavaHelper.is(node, type);
    }
    
    /**
     * Return true if all children of the given node match the given predicate, false otherwise.
     */
    public static final boolean allChildrenMatch(SpelNode node, Predicate<SpelNode> predicate) {
        return childrenStream(node).allMatch(predicate);
    }
    
    /**
     * Return a predicate that takes an {@link SpelNode} as input and returns true if all
     * children of that node match the given predicate.
     */
    public static final Predicate<SpelNode> allChildrenMatch(Predicate<SpelNode> predicate) {
        return node->allChildrenMatch(node, predicate);
    }
    
    /**
     * Return an {@link Iterable} that iterates over the children of the given {@link SpelNode}.
     */
    public static final Iterable<SpelNode> childrenIterable(SpelNode node) {
        return () -> new SpelNodeChildrenIterator(node);
    }
    
    /**
     * Return a {@link Stream} of children of the given SpelNode.
     */
    public static final Stream<SpelNode> childrenStream(SpelNode node) {
        return StreamSupport.stream(childrenIterable(node).spliterator(), false);
    }
    
    /**
     * Return an optional stream if all children of the given node are of the specified
     * type; the returned stream casts each child to the given type and then applies
     * the given {@link Function}.
     */
    public static final <T extends SpelNode, R> Optional<Stream<R>> mapChildrenStream(SpelNode node, Class<T> nodeType, Function<T,R> f) {
        return !allChildrenMatch(node, is(nodeType)) 
                ? Optional.empty()
                : Optional.of(childrenStream(node).map(nodeType::cast).map(f));
    }
    
    /**
     * Return an optional stream if all children of the given node are of the specified
     * type; the returned stream casts each child to the given type.
     */
    public static final <T extends SpelNode> Optional<Stream<T>> mapChildrenStream(SpelNode node, Class<T> type) {
        return mapChildrenStream(node, type, Function.identity());
    }
    
    public static final <T extends SpelNode, I, R> Optional<R> collectChildren(SpelNode node, Class<T> nodeType, Function<T,I> f, Collector<I,?,R> collector) {
        return mapChildrenStream(node, nodeType, f).map(s->s.collect(collector));
    }
    
    /**
     * {@link Iterator} implementation that iterators over the children of the
     * {@link SpelNode} passed in the constructor.
     * @author rsenden
     *
     */
    @RequiredArgsConstructor
    private static final class SpelNodeChildrenIterator implements Iterator<SpelNode> {
        private final SpelNode node;
        private int currentIndex = 0;
        @Override
        public boolean hasNext() {
            return currentIndex < node.getChildCount();
        }

        @Override
        public SpelNode next() {
            return node.getChild(currentIndex++);
        }
    }
}
