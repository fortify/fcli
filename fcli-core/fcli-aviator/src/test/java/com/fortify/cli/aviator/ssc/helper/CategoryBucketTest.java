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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;

class CategoryBucketTest {

    @Test
    void testNewBucket_isEmpty() {
        var bucket = new CategoryBucket("SQL Injection");
        assertEquals("SQL Injection", bucket.getCategory());
        assertTrue(bucket.getSastFindings().isEmpty());
        assertTrue(bucket.getDastFindings().isEmpty());
        assertEquals(0, bucket.getSastCount());
        assertEquals(0, bucket.getDastCount());
    }

    @Test
    void testSastOnly() {
        var bucket = new CategoryBucket("SQL Injection");
        bucket.addSastFinding(createVuln("INST-1"), "SQL Injection");

        assertTrue(bucket.isSastOnly());
        assertFalse(bucket.isDastOnly());
        assertFalse(bucket.isMixed());
        assertEquals(1, bucket.getSastCount());
        assertEquals(0, bucket.getDastCount());
    }

    @Test
    void testDastOnly() {
        var bucket = new CategoryBucket("SQL Injection");
        bucket.addDastFinding(createDastIssue("DAST-1", "SQL Injection"), "SQL Injection");

        assertFalse(bucket.isSastOnly());
        assertTrue(bucket.isDastOnly());
        assertFalse(bucket.isMixed());
        assertEquals(0, bucket.getSastCount());
        assertEquals(1, bucket.getDastCount());
    }

    @Test
    void testMixed() {
        var bucket = new CategoryBucket("SQL Injection");
        bucket.addSastFinding(createVuln("INST-1"), "SQL Injection");
        bucket.addDastFinding(createDastIssue("DAST-1", "SQL Injection"), "SQL Injection");

        assertFalse(bucket.isSastOnly());
        assertFalse(bucket.isDastOnly());
        assertTrue(bucket.isMixed());
        assertEquals(1, bucket.getSastCount());
        assertEquals(1, bucket.getDastCount());
    }

    @Test
    void testHasDifferentCategories_sameSastAndDast() {
        var bucket = new CategoryBucket("SQL Injection");
        bucket.addSastFinding(createVuln("INST-1"), "SQL Injection");
        bucket.addDastFinding(createDastIssue("DAST-1", "SQL Injection"), "SQL Injection");

        assertFalse(bucket.hasDifferentCategories());
    }

    @Test
    void testHasDifferentCategories_differentSastAndDast() {
        var bucket = new CategoryBucket("Cross-Frame Scripting");
        bucket.addSastFinding(createVuln("INST-1"), "Cross-Frame Scripting");
        bucket.addDastFinding(createDastIssue("DAST-1", "HTML5: Missing Framing Protection"),
            "HTML5: Missing Framing Protection");

        assertTrue(bucket.hasDifferentCategories());
    }

    @Test
    void testHasDifferentCategories_noSast() {
        var bucket = new CategoryBucket("SQL Injection");
        bucket.addDastFinding(createDastIssue("DAST-1", "SQL Injection"), "SQL Injection");
        assertFalse(bucket.hasDifferentCategories());
    }

    @Test
    void testHasDifferentCategories_noDast() {
        var bucket = new CategoryBucket("SQL Injection");
        bucket.addSastFinding(createVuln("INST-1"), "SQL Injection");
        assertFalse(bucket.hasDifferentCategories());
    }

    @Test
    void testCategoryDisplay_sameCategories() {
        var bucket = new CategoryBucket("SQL Injection");
        bucket.addSastFinding(createVuln("INST-1"), "SQL Injection");
        bucket.addDastFinding(createDastIssue("DAST-1", "SQL Injection"), "SQL Injection");

        assertEquals("SQL Injection", bucket.getSastCategoryDisplay());
        assertEquals("SQL Injection", bucket.getDastCategoryDisplay());
    }

    @Test
    void testCategoryDisplay_emptyFallsBackToCanonical() {
        var bucket = new CategoryBucket("SQL Injection");
        assertEquals("SQL Injection", bucket.getSastCategoryDisplay());
        assertEquals("SQL Injection", bucket.getDastCategoryDisplay());
    }

    @Test
    void testMultipleFindings() {
        var bucket = new CategoryBucket("SQL Injection");
        bucket.addSastFinding(createVuln("INST-1"), "SQL Injection");
        bucket.addSastFinding(createVuln("INST-2"), "SQL Injection");
        bucket.addDastFinding(createDastIssue("DAST-1", "SQL Injection"), "SQL Injection");
        bucket.addDastFinding(createDastIssue("DAST-2", "SQL Injection"), "SQL Injection");
        bucket.addDastFinding(createDastIssue("DAST-3", "SQL Injection"), "SQL Injection");

        assertEquals(2, bucket.getSastCount());
        assertEquals(3, bucket.getDastCount());
        assertTrue(bucket.isMixed());
    }

    private Vulnerability createVuln(String instanceId) {
        return Vulnerability.builder().instanceID(instanceId).build();
    }

    private DastIssue createDastIssue(String id, String category) {
        var issue = new DastIssue();
        issue.setId(id);
        issue.setCategory(category);
        return issue;
    }
}
