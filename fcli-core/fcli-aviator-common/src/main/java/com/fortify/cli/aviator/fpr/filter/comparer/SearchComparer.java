package com.fortify.cli.aviator.fpr.filter.comparer;

public interface SearchComparer {
    boolean matches(Object attributeValue);
    String getSearchTerm();
}