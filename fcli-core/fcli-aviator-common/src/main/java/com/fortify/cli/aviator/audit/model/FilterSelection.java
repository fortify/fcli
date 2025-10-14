package com.fortify.cli.aviator.audit.model;

import com.fortify.cli.aviator.fpr.filter.FilterSet;

import java.util.List;

public final class FilterSelection {
    private final FilterSet activeFilterSet;
    private final List<String> targetFolderNames;

    public FilterSelection(FilterSet activeFilterSet, List<String> targetFolderNames) {
        this.activeFilterSet = activeFilterSet;
        this.targetFolderNames = targetFolderNames;
    }

    public FilterSet getActiveFilterSet() {
        return activeFilterSet;
    }

    public List<String> getTargetFolderNames() {
        return targetFolderNames;
    }

    public boolean isFilteringByFolder() {
        return targetFolderNames != null && !targetFolderNames.isEmpty();
    }
}