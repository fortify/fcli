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