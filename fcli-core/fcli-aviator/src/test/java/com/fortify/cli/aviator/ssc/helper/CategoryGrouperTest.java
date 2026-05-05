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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;

class CategoryGrouperTest {

    @Test
    void testGroupFindings_singleMixedCategory() {
        var grouper = new CategoryGrouper();
        List<Vulnerability> sast = List.of(
            createVuln("INST-1", "SQL Injection", null)
        );
        List<DastIssue> dast = List.of(
            createDastIssue("DAST-1", "SQL Injection")
        );

        grouper.groupFindings(sast, dast);

        assertEquals(0, grouper.getSastOnlyBuckets().size());
        assertEquals(0, grouper.getDastOnlyBuckets().size());
        assertEquals(1, grouper.getMixedBuckets().size());
        assertEquals("SQL Injection", grouper.getMixedBuckets().get(0).getCategory());
    }

    @Test
    void testGroupFindings_sastOnlyAndDastOnly() {
        var grouper = new CategoryGrouper();
        List<Vulnerability> sast = List.of(
            createVuln("INST-1", "SQL Injection", null)
        );
        List<DastIssue> dast = List.of(
            createDastIssue("DAST-1", "Cross-Site Scripting")
        );

        grouper.groupFindings(sast, dast);

        assertEquals(1, grouper.getSastOnlyBuckets().size());
        assertEquals(1, grouper.getDastOnlyBuckets().size());
        assertEquals(0, grouper.getMixedBuckets().size());
    }

    @Test
    void testGroupFindings_equivalentCategories() {
        var grouper = new CategoryGrouper();
        List<Vulnerability> sast = List.of(
            createVuln("INST-1", "Cross-Frame Scripting", null)
        );
        List<DastIssue> dast = List.of(
            createDastIssue("DAST-1", "HTML5: Missing Framing Protection")
        );

        grouper.groupFindings(sast, dast);

        assertEquals(0, grouper.getSastOnlyBuckets().size());
        assertEquals(0, grouper.getDastOnlyBuckets().size());
        assertEquals(1, grouper.getMixedBuckets().size(),
            "Equivalent categories should be grouped into a single mixed bucket");

        var mixedBucket = grouper.getMixedBuckets().get(0);
        assertEquals(1, mixedBucket.getSastCount());
        assertEquals(1, mixedBucket.getDastCount());
        assertTrue(mixedBucket.hasDifferentCategories(),
            "Equivalent but differently-named categories should be flagged");
    }

    @Test
    void testGroupFindings_emptyLists() {
        var grouper = new CategoryGrouper();
        grouper.groupFindings(List.of(), List.of());

        assertEquals(0, grouper.getSastOnlyBuckets().size());
        assertEquals(0, grouper.getDastOnlyBuckets().size());
        assertEquals(0, grouper.getMixedBuckets().size());
    }

    @Test
    void testGroupFindings_nullCategory() {
        var grouper = new CategoryGrouper();
        List<Vulnerability> sast = List.of(
            createVuln("INST-1", null, null)
        );
        List<DastIssue> dast = List.of(
            createDastIssue("DAST-1", null)
        );

        grouper.groupFindings(sast, dast);

        assertEquals(0, grouper.getSastOnlyBuckets().size());
        assertEquals(0, grouper.getDastOnlyBuckets().size());
        assertEquals(0, grouper.getMixedBuckets().size(),
            "Findings with null category should be skipped");
    }

    @Test
    void testGroupFindings_emptyCategory() {
        var grouper = new CategoryGrouper();
        List<Vulnerability> sast = List.of(
            createVuln("INST-1", "", null)
        );
        List<DastIssue> dast = List.of(
            createDastIssue("DAST-1", "")
        );

        grouper.groupFindings(sast, dast);

        assertEquals(0, grouper.getSastOnlyBuckets().size());
        assertEquals(0, grouper.getDastOnlyBuckets().size());
        assertEquals(0, grouper.getMixedBuckets().size(),
            "Findings with empty category should be skipped");
    }

    @Test
    void testGroupFindings_multipleCategories() {
        var grouper = new CategoryGrouper();
        List<Vulnerability> sast = List.of(
            createVuln("INST-1", "SQL Injection", null),
            createVuln("INST-2", "SQL Injection", null),
            createVuln("INST-3", "Cross-Site Scripting", "Reflected")
        );
        List<DastIssue> dast = List.of(
            createDastIssue("DAST-1", "SQL Injection"),
            createDastIssue("DAST-2", "Path Traversal")
        );

        grouper.groupFindings(sast, dast);

        assertEquals(1, grouper.getSastOnlyBuckets().size(), "XSS should be SAST-only");
        assertEquals(1, grouper.getDastOnlyBuckets().size(), "Path Traversal should be DAST-only");
        assertEquals(1, grouper.getMixedBuckets().size(), "SQL Injection should be mixed");

        var mixedBucket = grouper.getMixedBuckets().get(0);
        assertEquals(2, mixedBucket.getSastCount());
        assertEquals(1, mixedBucket.getDastCount());
    }

    @Test
    void testGroupFindings_typeAndSubType() {
        var grouper = new CategoryGrouper();
        List<Vulnerability> sast = List.of(
            createVuln("INST-1", "Cross-Site Scripting", "Reflected")
        );
        List<DastIssue> dast = List.of(
            createDastIssue("DAST-1", "Cross-Site Scripting: Reflected")
        );

        grouper.groupFindings(sast, dast);

        assertEquals(1, grouper.getMixedBuckets().size(),
            "SAST Type+SubType should match DAST 'Type: SubType' category");
    }

    @Test
    void testGetSASTonlyFinding() {
        var grouper = new CategoryGrouper();
        List<Vulnerability> sast = List.of(
            createVuln("INST-1", "SQL Injection", null),
            createVuln("INST-2", "Cross-Site Scripting", null),
            createVuln("INST-3", "Cross-Site Scripting", null)
        );
        List<DastIssue> dast = List.of(
            createDastIssue("DAST-1", "SQL Injection")
        );

        grouper.groupFindings(sast, dast);

        assertEquals(2, grouper.getSASTonlyFinding(),
            "Should count only SAST findings in SAST-only buckets");
    }

    private Vulnerability createVuln(String instanceId, String type, String subType) {
        return Vulnerability.builder()
            .instanceID(instanceId)
            .type(type)
            .subType(subType)
            .build();
    }

    private DastIssue createDastIssue(String id, String category) {
        var issue = new DastIssue();
        issue.setId(id);
        issue.setCategory(category);
        return issue;
    }
}
