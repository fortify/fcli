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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;

/**
 * Groups SAST and DAST findings by category and classifies buckets.
 */
public class CategoryGrouper {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryGrouper.class);

    private final Map<String, CategoryBucket> buckets = new HashMap<>();

    /**
     * Groups all SAST and DAST findings by their category.
     *
     * Uses CategoryEquivalence to group findings with equivalent categories
     * into the same bucket. For example, "Cross-Frame Scripting" and
     * "HTML5: Missing Frame Protection" will be grouped together.
     *
     * @param sastFindings List of SAST vulnerabilities
     * @param dastFindings List of DAST issues
     */
    public void groupFindings(List<Vulnerability> sastFindings, List<DastIssue> dastFindings) {
        // Group SAST findings by canonical category
        for (Vulnerability vuln : sastFindings) {
            String originalCategory = getSastCategory(vuln);
            if (originalCategory != null && !originalCategory.isEmpty()) {
                String canonicalCategory = CategoryEquivalence.getCanonical(originalCategory);
                CategoryBucket bucket = buckets.computeIfAbsent(canonicalCategory, CategoryBucket::new);
                bucket.addSastFinding(vuln, originalCategory);
            }
        }

        // Group DAST findings by canonical category
        for (DastIssue issue : dastFindings) {
            String originalCategory = issue.getCategory();
            if (originalCategory != null && !originalCategory.isEmpty()) {
                String canonicalCategory = CategoryEquivalence.getCanonical(originalCategory);
                CategoryBucket bucket = buckets.computeIfAbsent(canonicalCategory, CategoryBucket::new);
                bucket.addDastFinding(issue, originalCategory);
            }
        }
    }

    /**
     * Extracts the category from a SAST vulnerability.
     * Uses Type and SubType, combining them with a colon if SubType exists.
     */
    private String getSastCategory(Vulnerability vuln) {
        String type = vuln.getType();
        String subType = vuln.getSubType();

        if (type == null || type.isEmpty()) {
            return null;
        }

        if (subType != null && !subType.isEmpty()) {
            return type + ": " + subType;
        }
        return type;
    }

    /**
     * Returns all buckets that contain only SAST findings.
     */
    public List<CategoryBucket> getSastOnlyBuckets() {
        List<CategoryBucket> result = new ArrayList<>();
        for (CategoryBucket bucket : buckets.values()) {
            if (bucket.isSastOnly()) {
                result.add(bucket);
            }
        }
        return result;
    }

    /**
     * Returns all buckets that contain only DAST findings.
     */
    public List<CategoryBucket> getDastOnlyBuckets() {
        List<CategoryBucket> result = new ArrayList<>();
        for (CategoryBucket bucket : buckets.values()) {
            if (bucket.isDastOnly()) {
                result.add(bucket);
            }
        }
        return result;
    }

    /**
     * Returns all buckets that contain both SAST and DAST findings.
     */
    public List<CategoryBucket> getMixedBuckets() {
        List<CategoryBucket> result = new ArrayList<>();
        for (CategoryBucket bucket : buckets.values()) {
            if (bucket.isMixed()) {
                result.add(bucket);
            }
        }
        return result;
    }

    /**
     * Prints the grouping statistics to the log.
     */
    public void printStatistics() {
        List<CategoryBucket> sastOnly = getSastOnlyBuckets();
        List<CategoryBucket> dastOnly = getDastOnlyBuckets();
        List<CategoryBucket> mixed = getMixedBuckets();

        // Count total findings in SAST-only buckets
        int sastOnlyFindings = 0;
        for (CategoryBucket bucket : sastOnly) {
            sastOnlyFindings += bucket.getSastCount();
        }

        // Count total findings in DAST-only buckets
        int dastOnlyFindings = 0;
        for (CategoryBucket bucket : dastOnly) {
            dastOnlyFindings += bucket.getDastCount();
        }

        // Count total findings in mixed buckets (SAST + DAST)
        int mixedFindings = 0;
        for (CategoryBucket bucket : mixed) {
            mixedFindings += bucket.getSastCount() + bucket.getDastCount();
        }

        int totalCategories = sastOnly.size() + dastOnly.size() + mixed.size();
        int totalFindings = sastOnlyFindings + dastOnlyFindings + mixedFindings;

        LOG.info("");
        LOG.info("=== Category Grouping Results ===");
        LOG.info("");
        LOG.info("+-------------------+------------+----------+");
        LOG.info("| Bucket Type       | Categories | Findings |");
        LOG.info("+-------------------+------------+----------+");
        LOG.info(String.format("| SAST-only         | %10d | %8d |", sastOnly.size(), sastOnlyFindings));
        LOG.info(String.format("| DAST-only         | %10d | %8d |", dastOnly.size(), dastOnlyFindings));
        LOG.info(String.format("| Mixed             | %10d | %8d |", mixed.size(), mixedFindings));
        LOG.info("+-------------------+------------+----------+");
        LOG.info(String.format("| TOTAL             | %10d | %8d |", totalCategories, totalFindings));
        LOG.info("+-------------------+------------+----------+");
        LOG.info("");

        if (!mixed.isEmpty()) {
            printMixedCategoriesTable(mixed);
        }
    }

    /**
     * Prints the mixed categories as a formatted table to the log.
     */
    private void printMixedCategoriesTable(List<CategoryBucket> mixed) {
        // First, check if any buckets have different SAST/DAST categories
        boolean hasEquivalentCategories = false;
        for (CategoryBucket bucket : mixed) {
            if (bucket.hasDifferentCategories()) {
                hasEquivalentCategories = true;
                break;
            }
        }

        // Build display strings and calculate column widths
        List<String> categoryDisplays = new ArrayList<>();
        int categoryWidth = "Category".length();
        for (CategoryBucket bucket : mixed) {
            String display;
            if (bucket.hasDifferentCategories()) {
                display = bucket.getSastCategoryDisplay() + " / " + bucket.getDastCategoryDisplay();
            } else {
                display = bucket.getCategory();
            }
            categoryDisplays.add(display);
            categoryWidth = Math.max(categoryWidth, display.length());
        }

        int sastWidth = 6;  // "SAST" + padding
        int dastWidth = 6;  // "DAST" + padding
        int totalWidth = 7; // "Total" + padding

        // Build format strings
        String rowFormat = "| %-" + categoryWidth + "s | %" + sastWidth + "d | %" + dastWidth + "d | %" + totalWidth + "d |";
        String headerFormat = "| %-" + categoryWidth + "s | %" + sastWidth + "s | %" + dastWidth + "s | %" + totalWidth + "s |";

        // Build separator line
        StringBuilder separator = new StringBuilder("+");
        separator.append("-".repeat(categoryWidth + 2));
        separator.append("+");
        separator.append("-".repeat(sastWidth + 2));
        separator.append("+");
        separator.append("-".repeat(dastWidth + 2));
        separator.append("+");
        separator.append("-".repeat(totalWidth + 2));
        separator.append("+");

        // Calculate totals
        int totalSast = 0;
        int totalDast = 0;
        for (CategoryBucket bucket : mixed) {
            totalSast += bucket.getSastCount();
            totalDast += bucket.getDastCount();
        }

        LOG.info("Mixed categories (correlation candidates):");
        if (hasEquivalentCategories) {
            LOG.info("(Categories shown as 'SAST category / DAST category' where they differ)");
        }
        LOG.info(separator.toString());
        LOG.info(String.format(headerFormat, "Category", "SAST", "DAST", "Total"));
        LOG.info(separator.toString());
        int i = 0;
        for (CategoryBucket bucket : mixed) {
            int rowTotal = bucket.getSastCount() + bucket.getDastCount();
            LOG.info(String.format(rowFormat, categoryDisplays.get(i), bucket.getSastCount(), bucket.getDastCount(), rowTotal));
            i++;
        }
        LOG.info(separator.toString());
        LOG.info(String.format(rowFormat, "TOTAL", totalSast, totalDast, totalSast + totalDast));
        LOG.info(separator.toString());
    }
}
