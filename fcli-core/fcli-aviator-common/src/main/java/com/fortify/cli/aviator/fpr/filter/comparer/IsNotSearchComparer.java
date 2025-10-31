/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
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