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
package com.fortify.cli.aviator.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator._common.config.AviatorConfigManager;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.grpc.DastAuditResult;
import com.fortify.cli.aviator.grpc.DastAuditStreamConfig;
import com.fortify.cli.aviator.grpc.DastAuditStreamResult;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.ResourceUtil;

class DastAuditFPRTest {
    @TempDir Path tempDir;

    @Test
    void auditsEligibleFindingAndWritesConservativeXml() throws Exception {
        Path fpr = createFpr();
        var config = new DastAuditStreamConfig("token", "app", "ssc", "1", null);

        DastAuditFprResult result;
        try (FprHandle handle = new FprHandle(fpr)) {
            result = DastAuditFPR.audit(handle, config, defaultTagMapping(), (ignoredConfig, items, total) ->
                CompletableFuture.completedFuture(new DastAuditStreamResult(List.of(
                    new DastAuditResult.Success("DAST-1", false, "HIGH", "reason", "", "comment", "", "")
                ), 1, 0, false, null, null)));
        }

        assertEquals("AUDITED", result.status());
        assertEquals(1, result.falsePositivesSuppressed());
        try (FileSystem zip = FileSystems.newFileSystem(fpr)) {
            String auditXml = Files.readString(zip.getPath("/audit.xml"));
            assertTrue(auditXml.contains("instanceId=\"DAST-1\""));
            assertTrue(auditXml.contains("suppressed=\"true\""));
            assertTrue(auditXml.contains("PROCESSED_BY_AVIATOR"));
            assertFalse(Files.exists(zip.getPath("/remediations.xml")));
        }
    }

    @Test
    void customTagMappingControlsFinalTagAndSuppression() throws Exception {
        Path fpr = createFpr();
        Path tagMapping = tempDir.resolve("dast-tag-mapping.yaml");
        Files.writeString(tagMapping, """
            tag_id: "custom-analysis-tag"
            mapping:
              tier_1:
                fp: { value: "Confirmed FP", suppress: false }
                tp: { value: "Confirmed TP", suppress: false }
                unsure: { suppress: false }
              tier_2:
                fp: { value: "Review FP", suppress: true }
                tp: { value: "Review TP", suppress: false }
                unsure: { suppress: false }
            """);
        var config = new DastAuditStreamConfig("token", "app", "ssc", "1", null);

        DastAuditFprResult result;
        try (FprHandle handle = new FprHandle(fpr)) {
            result = DastAuditFPR.audit(handle, config,
                ResourceUtil.loadYamlFile(tagMapping.toFile(), TagMappingConfig.class),
                (ignoredConfig, items, total) ->
                CompletableFuture.completedFuture(new DastAuditStreamResult(List.of(
                    new DastAuditResult.Success("DAST-1", false, "MEDIUM", "reason", "", "comment", "", "")
                ), 1, 0, false, null, null)));
        }

        assertEquals(1, result.falsePositivesSuppressed());
        try (FileSystem zip = FileSystems.newFileSystem(fpr)) {
            String auditXml = Files.readString(zip.getPath("/audit.xml"));
            assertTrue(auditXml.contains("suppressed=\"true\""));
            assertTrue(auditXml.contains("id=\"custom-analysis-tag\""));
            assertTrue(auditXml.contains(">Review FP<"));
        }
    }

    @Test
    void writesOnlyIssuesUpdatedByCurrentAudit() throws Exception {
        Path fpr = createFpr();
        try (FileSystem zip = FileSystems.newFileSystem(fpr)) {
            Files.writeString(zip.getPath("/audit.xml"), """
                <Audit xmlns="xmlns://www.fortify.com/schema/audit"><IssueList>
                  <Issue instanceId="DAST-1" revision="0" suppressed="false"/>
                  <Issue instanceId="DAST-2" revision="7" suppressed="false"/>
                </IssueList></Audit>
                """);
        }
        var config = new DastAuditStreamConfig("token", "app", "ssc", "1", null);

        try (FprHandle handle = new FprHandle(fpr)) {
            DastAuditFPR.audit(handle, config, defaultTagMapping(), (ignoredConfig, items, total) ->
                CompletableFuture.completedFuture(new DastAuditStreamResult(List.of(
                    new DastAuditResult.Success("DAST-1", true, "HIGH", "reason", "", "comment", "", "")
                ), 1, 0, false, null, null)));
        }

        try (FileSystem zip = FileSystems.newFileSystem(fpr)) {
            String auditXml = Files.readString(zip.getPath("/audit.xml"));
            assertTrue(auditXml.contains("instanceId=\"DAST-1\""));
            assertFalse(auditXml.contains("instanceId=\"DAST-2\""));
        }
    }

    @Test
    void missingTerminalResponseIsCountedAsFailure() throws Exception {
        Path fpr = createFpr();
        var config = new DastAuditStreamConfig("token", "app", "ssc", "1", null);

        try (FprHandle handle = new FprHandle(fpr)) {
            DastAuditFprResult result = DastAuditFPR.audit(
                handle, config, defaultTagMapping(), (ignoredConfig, items, total) ->
                CompletableFuture.completedFuture(new DastAuditStreamResult(
                    List.of(), 1, 0, false, null, null)));

            assertEquals("FAILED", result.status());
            assertEquals(1, result.failed());
        }
    }

    private TagMappingConfig defaultTagMapping() {
        return AviatorConfigManager.getInstance().getDefaultDastTagMappingConfig();
    }

    private Path createFpr() throws Exception {
        Path fpr = tempDir.resolve("dast.fpr");
        try (FileSystem zip = FileSystems.newFileSystem(fpr, Map.of("create", "true"))) {
            Files.writeString(zip.getPath("/webinspect.xml"), """
                <WebInspectScan><Session requestId="REQ-1"><URL>https://example.test</URL><Issues>
                <Issue id="DAST-1"><Name>SQL Injection</Name><Severity>4</Severity></Issue>
                </Issues></Session></WebInspectScan>
                """, StandardCharsets.UTF_8);
        }
        return fpr;
    }
}