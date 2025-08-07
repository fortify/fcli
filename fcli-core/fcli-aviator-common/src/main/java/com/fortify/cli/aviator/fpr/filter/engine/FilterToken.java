package com.fortify.cli.aviator.fpr.filter.engine;

/**
 * Represents a token parsed from a filter query string.
 */
public class FilterToken {

    public enum TokenType {
        MODIFIER_TERM, // e.g., category:"SQL Injection"
        OPERATOR_AND,
        OPERATOR_OR
    }

    private final TokenType type;
    private final Object value; // Will be a SearchQuery for MODIFIER_TERM or null for operators

    public FilterToken(TokenType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }
}