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
package com.fortify.cli.aviator.audit.model;

import java.util.List;

import com.fortify.cli.aviator.fpr.filter.FilterSet;

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