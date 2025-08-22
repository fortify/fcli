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