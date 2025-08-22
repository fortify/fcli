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
    public boolean matches(Object possibleMatch) {
        //if possible match is null hten it definitely IS NOT
        if (possibleMatch == null) {
            return true;
        }
        boolean result = !wrapped.matches(possibleMatch);
        return result;
    }

    public String getSearchTerm() {
        return "!" + wrapped.getSearchTerm();
    }
}