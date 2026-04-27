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
package com.fortify.cli.aviator.fpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.filter.VulnerabilityFilterer;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;

class FPRProcessorTest {
    private static final String CUSTOM_TAG_ID = "11111111-2222-3333-4444-555555555555";

    private Path tempFprFile;
    private FprHandle fprHandle;

    @AfterEach
    void tearDown() throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
        }
        if (tempFprFile != null) {
            Files.deleteIfExists(tempFprFile);
        }
    }

    @Test
    void testProcessMergesAuditDerivedStateIntoStreamingVulnerabilities() throws Exception {
        createTestFpr(minimalAuditFvdl());

        AuditIssue auditIssue = AuditIssue.builder()
            .instanceId("instance-1")
            .suppressed(true)
            .tags(Map.of(Constants.AUDITOR_STATUS_TAG_ID, Constants.NOT_AN_ISSUE))
            .threadedComments(List.of(AuditIssue.Comment.builder()
                .content("Reviewed by analyst")
                .username("analyst")
                .timestamp("2026-04-22T00:00:00Z")
                .build()))
            .build();

        FPRProcessor fprProcessor = new FPRProcessor(fprHandle, Map.of("instance-1", auditIssue), null);
        List<Vulnerability> vulnerabilities = fprProcessor.process(new StreamingFVDLProcessor(fprHandle));

        assertEquals(1, vulnerabilities.size());

        Vulnerability vulnerability = vulnerabilities.get(0);
        assertTrue(vulnerability.isSuppressed());
        assertTrue(vulnerability.isAudited());
        assertEquals(Constants.NOT_AN_ISSUE, vulnerability.getIssueStatus());
        assertEquals("Reviewed by analyst", vulnerability.getLastComment());
        assertEquals(1, VulnerabilityFilterer.filter(vulnerabilities, "suppressed:true").size());
        assertEquals(1, VulnerabilityFilterer.filter(vulnerabilities, "audited:true").size());
        assertEquals(1, VulnerabilityFilterer.filter(vulnerabilities, "[issue status]:\"Not an Issue\"").size());
        assertEquals(1, VulnerabilityFilterer.filter(vulnerabilities, "commentuser:analyst").size());
        assertEquals(1, VulnerabilityFilterer.filter(vulnerabilities, "historyuser:analyst").size());
    }

    @Test
    void testProcessDoesNotMarkCommentOnlyIssueAsAudited() throws Exception {
        createTestFpr(minimalAuditFvdl());

        AuditIssue auditIssue = AuditIssue.builder()
            .instanceId("instance-1")
            .threadedComments(List.of(AuditIssue.Comment.builder()
                .content("Investigating")
                .username("analyst")
                .timestamp("2026-04-22T00:00:00Z")
                .build()))
            .build();

        Vulnerability vulnerability = processSingleVulnerability(auditIssue);

        assertFalse(vulnerability.isAudited());
    }

    @Test
    void testProcessDoesNotMarkCustomTagOnlyIssueAsAudited() throws Exception {
        createTestFpr(minimalAuditFvdl());

        AuditIssue auditIssue = AuditIssue.builder()
            .instanceId("instance-1")
            .tags(Map.of(CUSTOM_TAG_ID, "High"))
            .build();

        Vulnerability vulnerability = processSingleVulnerability(auditIssue);

        assertFalse(vulnerability.isAudited());
    }

    @Test
    void testProcessDoesNotMarkPendingReviewDefaultAuditorStatusAsAudited() throws Exception {
        createTestFpr(minimalAuditFvdl());

        AuditIssue auditIssue = AuditIssue.builder()
            .instanceId("instance-1")
            .tags(Map.of(Constants.AUDITOR_STATUS_TAG_ID, Constants.PENDING_REVIEW))
            .build();

        Vulnerability vulnerability = processSingleVulnerability(auditIssue);

        assertFalse(vulnerability.isAudited());
    }

    private String minimalAuditFvdl() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <Vulnerabilities>
                    <Vulnerability>
                      <ClassInfo>
                        <ClassID>RULE-1</ClassID>
                        <Kingdom>Dataflow</Kingdom>
                        <Type>Cross-Site Scripting</Type>
                        <Subtype>Reflected</Subtype>
                        <AnalyzerName>Dataflow</AnalyzerName>
                        <DefaultSeverity>3.0</DefaultSeverity>
                      </ClassInfo>
                      <InstanceInfo>
                        <InstanceID>instance-1</InstanceID>
                        <InstanceSeverity>3.0</InstanceSeverity>
                        <Confidence>4.0</Confidence>
                      </InstanceInfo>
                    </Vulnerability>
                  </Vulnerabilities>
                </FVDL>
                """;
    }

    private void createTestFpr(String auditFvdlXml) throws Exception {
        tempFprFile = Files.createTempFile("fpr-processor", ".fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {
            zipOutputStream.putNextEntry(new ZipEntry("audit.fvdl"));
            zipOutputStream.write(auditFvdlXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zipOutputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index/>".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        fprHandle = new FprHandle(tempFprFile);
    }

    private Vulnerability processSingleVulnerability(AuditIssue auditIssue) throws Exception {
        FPRProcessor fprProcessor = new FPRProcessor(fprHandle, Map.of("instance-1", auditIssue), new AuditProcessor(fprHandle));
        List<Vulnerability> vulnerabilities = fprProcessor.process(new StreamingFVDLProcessor(fprHandle));
        assertEquals(1, vulnerabilities.size());
        return vulnerabilities.get(0);
    }
}