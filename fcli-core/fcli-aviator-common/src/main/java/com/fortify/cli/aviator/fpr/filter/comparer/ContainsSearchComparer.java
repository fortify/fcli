package com.fortify.cli.aviator.fpr.filter.comparer;

public class ContainsSearchComparer implements SearchComparer {
    private final String searchTerm;

    public ContainsSearchComparer(String searchTerm) {
        this.searchTerm = searchTerm.toLowerCase();
    }

    @Override
    public boolean matches(Object attributeValue) {
        if (!(attributeValue instanceof String)) {
            return false;
        }
        return ((String) attributeValue).toLowerCase().contains(searchTerm);
    }
}