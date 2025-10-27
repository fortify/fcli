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
 * Implements a case-insensitive "exact match" search. The search term is stored internally.
 */
public class ExactMatchComparer implements SearchComparer {
    private final String searchTerm;

    public ExactMatchComparer(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    @Override
    public boolean matches(Object attributeValue) {
        if (searchTerm == null) {
            return attributeValue == null;
        }
        if (!(attributeValue instanceof String)) {
            return false;
        }
        boolean result = searchTerm.equalsIgnoreCase((String) attributeValue);
        return result;
    }

    public String getSearchTerm() {
        return searchTerm;
    }
}