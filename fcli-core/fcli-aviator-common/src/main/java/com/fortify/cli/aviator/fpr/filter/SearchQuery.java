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
package com.fortify.cli.aviator.fpr.filter;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.comparer.SearchComparer;

import lombok.Getter;

/**
 * Represents a single, atomic condition in a filter, combining an attribute with a self-contained comparer.
 */
@Getter
public class SearchQuery {
    private final String attributeName;
    private final SearchComparer comparer;

    public SearchQuery(String attributeName, SearchComparer comparer) {
        this.attributeName = attributeName;
        this.comparer = comparer;
    }

    /**
     * Evaluates this single query against a vulnerability.
     */
    public boolean evaluate(Vulnerability vuln) {
        if (vuln == null) {
            return false;
        }
        Object attributeValue = vuln.getAttributeValue(attributeName);
        return comparer.matches(attributeValue);
    }

    public SearchComparer getSearchComparer() {
        return comparer;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String toString() {
        return attributeName + ":" + comparer.toString();
    }
}