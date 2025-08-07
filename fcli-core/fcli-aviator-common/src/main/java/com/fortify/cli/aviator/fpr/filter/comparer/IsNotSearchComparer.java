package com.fortify.cli.aviator.fpr.filter.comparer;

/**
 * A decorator that negates the result of another SearchComparer.
 */
public class IsNotSearchComparer implements SearchComparer {
    private final SearchComparer wrapped;

    public IsNotSearchComparer(SearchComparer wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean matches(Object attributeValue) {
        return !wrapped.matches(attributeValue);
    }
}