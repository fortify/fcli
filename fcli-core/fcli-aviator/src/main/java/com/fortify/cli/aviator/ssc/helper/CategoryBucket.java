/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.aviator.ssc.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;

import lombok.Getter;

/**
 * Represents a bucket of findings grouped by category.
 * Can contain SAST findings, DAST findings, or both.
 *
 * The bucket uses a canonical category for grouping (via CategoryEquivalence),
 * but also tracks the actual SAST and DAST categories for display purposes
 * when they differ.
 */
@Getter
public class CategoryBucket {
    /**
     * The canonical category used for grouping (from CategoryEquivalence).
     */
    private final String category;

    /**
     * The actual category names present in SAST findings (may differ from canonical).
     */
    private final Set<String> sastCategories = new HashSet<>();

    /**
     * The actual category names present in DAST findings (may differ from canonical).
     */
    private final Set<String> dastCategories = new HashSet<>();

    private final List<Vulnerability> sastFindings = new ArrayList<>();
    private final List<DastIssue> dastFindings = new ArrayList<>();

    public CategoryBucket(String category) {
        this.category = category;
    }

    public void addSastFinding(Vulnerability vuln, String originalCategory) {
        sastFindings.add(vuln);
        if (originalCategory != null) {
            sastCategories.add(originalCategory);
        }
    }

    public void addDastFinding(DastIssue issue, String originalCategory) {
        dastFindings.add(issue);
        if (originalCategory != null) {
            dastCategories.add(originalCategory);
        }
    }

    /**
     * Checks if SAST and DAST categories in this bucket differ.
     * This is used to determine whether to display both categories in the output.
     *
     * @return true if SAST and DAST have different category names
     */
    public boolean hasDifferentCategories() {
        if (sastCategories.isEmpty() || dastCategories.isEmpty()) {
            return false;
        }
        // If there's any SAST category not in DAST categories (or vice versa), they differ
        return !sastCategories.equals(dastCategories);
    }

    /**
     * Gets a display string for the SAST category/categories.
     */
    public String getSastCategoryDisplay() {
        if (sastCategories.isEmpty()) {
            return category;
        }
        return String.join(", ", sastCategories);
    }

    /**
     * Gets a display string for the DAST category/categories.
     */
    public String getDastCategoryDisplay() {
        if (dastCategories.isEmpty()) {
            return category;
        }
        return String.join(", ", dastCategories);
    }

    public boolean isSastOnly() {
        return !sastFindings.isEmpty() && dastFindings.isEmpty();
    }

    public boolean isDastOnly() {
        return sastFindings.isEmpty() && !dastFindings.isEmpty();
    }

    public boolean isMixed() {
        return !sastFindings.isEmpty() && !dastFindings.isEmpty();
    }

    public int getSastCount() {
        return sastFindings.size();
    }

    public int getDastCount() {
        return dastFindings.size();
    }
}
