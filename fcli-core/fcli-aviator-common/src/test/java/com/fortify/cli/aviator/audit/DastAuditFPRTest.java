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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditTier;
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
        var config = streamConfig();

        DastAuditFprResult result;
        try (FprHandle handle = new FprHandle(fpr)) {
            result = DastAuditFPR.audit(handle, config, defaultTagMapping(), (ignoredConfig, items, total) ->
                CompletableFuture.completedFuture(DastAuditStreamResult.builder()
                    .results(List.of(successResult(false, "HIGH", AuditTier.GOLD)))
                    .reservedQuota(1)
                    .build()));
        }

        assertEquals(DastAuditFprStatus.AUDITED, result.status());
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
        var config = streamConfig();

        DastAuditFprResult result;
        try (FprHandle handle = new FprHandle(fpr)) {
            result = DastAuditFPR.audit(handle, config,
                ResourceUtil.loadYamlFile(tagMapping.toFile(), TagMappingConfig.class),
                (ignoredConfig, items, total) ->
                CompletableFuture.completedFuture(DastAuditStreamResult.builder()
                    .results(List.of(successResult(false, "MEDIUM", AuditTier.SILVER)))
                    .reservedQuota(1)
                    .build()));
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
        var config = streamConfig();

        try (FprHandle handle = new FprHandle(fpr)) {
            DastAuditFPR.audit(handle, config, defaultTagMapping(), (ignoredConfig, items, total) ->
                CompletableFuture.completedFuture(DastAuditStreamResult.builder()
                    .results(List.of(successResult(true, "HIGH", AuditTier.GOLD)))
                    .reservedQuota(1)
                    .build()));
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
        var config = streamConfig();

        try (FprHandle handle = new FprHandle(fpr)) {
            DastAuditFprResult result = DastAuditFPR.audit(
                handle, config, defaultTagMapping(), (ignoredConfig, items, total) ->
                CompletableFuture.completedFuture(DastAuditStreamResult.builder()
                    .results(List.of())
                    .reservedQuota(1)
                    .build()));

            assertEquals(DastAuditFprStatus.FAILED, result.status());
            assertEquals(1, result.failed());
        }
    }

    @Test
    void excludesSuppressedAviatorProcessedAndHumanAuditedFindings() throws Exception {
        Path fpr = createEligibilityFpr();

        try (FprHandle handle = new FprHandle(fpr)) {
            DastAuditFprResult result = DastAuditFPR.audit(
                handle, streamConfig(), defaultTagMapping(), (ignoredConfig, items, total) -> {
                    assertEquals(List.of("DAST-5"), items.stream().map(item -> item.issue().getId()).toList());
                    return CompletableFuture.completedFuture(DastAuditStreamResult.builder()
                        .results(List.of(successResult("DAST-5", true, "HIGH", AuditTier.GOLD)))
                        .reservedQuota(1)
                        .build());
                });

            assertEquals(5, result.totalReported());
            assertEquals(1, result.submitted());
            assertEquals(4, result.skipped());
        }
    }

    @Test
    void allExcludedFindingsReturnSkippedWithoutStartingStream() throws Exception {
        Path fpr = createFpr();
        try (FileSystem zip = FileSystems.newFileSystem(fpr)) {
            Files.writeString(zip.getPath("/audit.xml"), """
                <Audit xmlns="xmlns://www.fortify.com/schema/audit"><IssueList>
                  <Issue instanceId="DAST-1" revision="0" suppressed="true"/>
                </IssueList></Audit>
                """);
        }

        try (FprHandle handle = new FprHandle(fpr)) {
            DastAuditFprResult result = DastAuditFPR.audit(
                handle, streamConfig(), defaultTagMapping(), (ignoredConfig, items, total) -> {
                    throw new AssertionError("Stream must not start when all findings are excluded");
                });

            assertEquals(DastAuditFprStatus.SKIPPED, result.status());
            assertEquals(1, result.totalReported());
            assertEquals(1, result.skipped());
            assertEquals(0, result.submitted());
        }
    }

    @Test
    void missingStreamFutureProducesTechnicalError() throws Exception {
        Path fpr = createFpr();

        try (FprHandle handle = new FprHandle(fpr)) {
            AviatorTechnicalException exception = assertThrows(AviatorTechnicalException.class,
                () -> DastAuditFPR.audit(handle, streamConfig(), defaultTagMapping(),
                    (ignoredConfig, items, total) -> null));

            assertEquals("DAST audit stream did not return a completion future", exception.getMessage());
        }
    }

    @Test
    void missingStreamResultProducesTechnicalError() throws Exception {
        Path fpr = createFpr();

        try (FprHandle handle = new FprHandle(fpr)) {
            AviatorTechnicalException exception = assertThrows(AviatorTechnicalException.class,
                () -> DastAuditFPR.audit(handle, streamConfig(), defaultTagMapping(),
                    (ignoredConfig, items, total) -> CompletableFuture.completedFuture(null)));

            assertEquals("DAST audit stream completed without a result", exception.getMessage());
        }
    }

    private DastAuditResult.Success successResult(boolean truePositive, String confidence, AuditTier tier) {
        return successResult("DAST-1", truePositive, confidence, tier);
    }

    private DastAuditResult.Success successResult(
            String issueId, boolean truePositive, String confidence, AuditTier tier) {
        return DastAuditResult.Success.builder()
            .issueId(issueId)
            .truePositive(truePositive)
            .confidence(confidence)
            .tier(tier)
            .reasoning("reason")
            .finalComment("comment")
            .build();
    }

    private TagMappingConfig defaultTagMapping() {
        return AviatorConfigManager.getInstance().getDefaultDastTagMappingConfig();
    }

    private DastAuditStreamConfig streamConfig() {
        return DastAuditStreamConfig.builder()
            .token("token")
            .applicationName("app")
            .sscApplicationName("ssc")
            .sscApplicationVersion("1")
            .build();
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

    private Path createEligibilityFpr() throws Exception {
        Path fpr = tempDir.resolve("dast-eligibility.fpr");
        try (FileSystem zip = FileSystems.newFileSystem(fpr, Map.of("create", "true"))) {
            Files.writeString(zip.getPath("/webinspect.xml"), """
                <WebInspectScan><Session requestId="REQ-1"><URL>https://example.test</URL><Issues>
                <Issue id="DAST-1"><Name>Suppressed</Name><Severity>4</Severity></Issue>
                <Issue id="DAST-2"><Name>Aviator status</Name><Severity>4</Severity></Issue>
                <Issue id="DAST-3"><Name>Legacy Aviator outcome</Name><Severity>4</Severity></Issue>
                <Issue id="DAST-4"><Name>Human audited</Name><Severity>4</Severity></Issue>
                <Issue id="DAST-5"><Name>Eligible</Name><Severity>4</Severity></Issue>
                </Issues></Session></WebInspectScan>
                """, StandardCharsets.UTF_8);
            Files.writeString(zip.getPath("/audit.xml"), """
                <Audit xmlns="xmlns://www.fortify.com/schema/audit"><IssueList>
                    <Issue instanceId="DAST-1" revision="0" suppressed="true"/>
                    <Issue instanceId="DAST-2" revision="0" suppressed="false">
                        <Tag id="FB7B0462-2C2E-46D9-811A-DCC1F3C83051"><Value>PROCESSED_BY_AVIATOR</Value></Tag>
                    </Issue>
                    <Issue instanceId="DAST-3" revision="0" suppressed="false">
                        <Tag id="013cc66f-8651-4e39-bacb-beb918c5ef65"><Value>Not an Issue</Value></Tag>
                    </Issue>
                    <Issue instanceId="DAST-4" revision="0" suppressed="false">
                        <Tag id="ACB05E55-E74D-468C-8501-52E1FDC27D71"><Value>Exploitable</Value></Tag>
                    </Issue>
                </IssueList></Audit>
                """, StandardCharsets.UTF_8);
        }
        return fpr;
    }
}