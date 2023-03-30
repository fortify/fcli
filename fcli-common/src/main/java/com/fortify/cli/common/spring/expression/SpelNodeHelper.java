package com.fortify.cli.common.spring.expression;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;

import lombok.RequiredArgsConstructor;

public class SpelNodeHelper {
    private SpelNodeHelper() {}
    
    public static final Literal getFirstLiteral(SpelNode... nodes) {
        return (Literal)Stream.of(nodes).filter(SpelNodeHelper::isLiteral).findFirst().orElse(null);
    }
    
    public static final boolean isLiteral(SpelNode node) {
        return node instanceof Literal;
    }
    
    public static final String getFirstQualifiedPropertyName(SpelNode... nodes) {
        return Stream.of(nodes).map(SpelNodeHelper::getQualifiedPropertyName)
                .filter(Objects::nonNull).findFirst().orElse(null);
    }
    
    public static final String getQualifiedPropertyName(SpelNode node) {
        return node instanceof PropertyOrFieldReference 
                ? ((PropertyOrFieldReference) node).getName() 
                : getCompoundPropertyName(node);
    }
    
    public static final String getCompoundPropertyName(SpelNode node) {
        if ( isCompoundPropertyReference(node) ) {
            return childrenStream(node).map(SpelNodeHelper::getQualifiedPropertyName)
                .collect(Collectors.joining("."));
        }
        return null;
    }
    
    public static final SpelNode getFirstPropertyReference(SpelNode... nodes) {
        return Stream.of(nodes).filter(SpelNodeHelper::isPropertyReference).findFirst().orElse(null);
    }
    
    public static final boolean isPropertyReference(SpelNode node) {
        return node instanceof PropertyOrFieldReference
                || isCompoundPropertyReference(node);
    }

    public static final boolean isCompoundPropertyReference(SpelNode node) {
        if ( node instanceof CompoundExpression ) {
            return childrenStream(node).allMatch(SpelNodeHelper::isPropertyReference);
        } 
        return false;
    }
    
    public static final Iterable<SpelNode> childrenIterable(SpelNode node) {
        return () -> new SpelNodeIterator(node);
    }
    
    public static final Stream<SpelNode> childrenStream(SpelNode node) {
        return StreamSupport.stream(childrenIterable(node).spliterator(), false);
    }
    
    @RequiredArgsConstructor
    private static final class SpelNodeIterator implements Iterator<SpelNode> {
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
