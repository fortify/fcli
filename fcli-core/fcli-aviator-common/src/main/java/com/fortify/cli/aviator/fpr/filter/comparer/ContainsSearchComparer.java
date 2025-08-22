package com.fortify.cli.aviator.fpr.filter.comparer;

public class ContainsSearchComparer implements SearchComparer {
    private final String searchTerm;

    public ContainsSearchComparer(String searchTerm) {
        String unescapedSearchTerm = searchTerm.replace("\\:", ":");
        this.searchTerm = unescapedSearchTerm.toLowerCase();
    }

    @Override
    public boolean matches(Object attributeValue) {
        if (!(attributeValue instanceof String)) {
            return false;
        }
        boolean result = ((String) attributeValue).toLowerCase().contains(searchTerm);
        return result;
    }

    public String getSearchTerm() {
        return searchTerm;
    }
}