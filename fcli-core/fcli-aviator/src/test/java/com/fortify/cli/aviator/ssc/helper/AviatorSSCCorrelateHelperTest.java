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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;

class AviatorSSCCorrelateHelperTest {

    @TempDir Path tempDir;

    // ── buildOutputJson ──────────────────────────────────────────────────

    @Test
    void testBuildOutputJson_withCorrelatedPairs() {
        var av = createAppVersionDescriptor("37", "MyApp", "1.0");
        List<CorrelatedPair> pairs = List.of(
            new CorrelatedPair("SAST-1", "DAST-1", "scan-guid", "HIGH", "match"),
            new CorrelatedPair("SAST-2", "DAST-2", "scan-guid", "MEDIUM", "match")
        );

        var result = AviatorSSCCorrelateHelper.buildOutputJson(av, "artifact-123", 5, 4, pairs, "CORRELATED");

        assertEquals("37", result.get("id").asText());
        assertEquals("MyApp", result.get("applicationName").asText());
        assertEquals("1.0", result.get("versionName").asText());
        assertEquals("artifact-123", result.get("artifactId").asText());
        assertEquals("CORRELATED", result.get(IActionCommandResultSupplier.actionFieldName).asText());

        JsonNode correlate = result.get("operation").get("correlate");
        assertEquals(5, correlate.get("submitted").asInt());
        assertEquals(4, correlate.get("succeeded").asInt());
        assertEquals(1, correlate.get("skipped").asInt());
        assertEquals(2, correlate.get("correlated").asInt());
        assertNotNull(correlate.get("message").asText());
    }

    @Test
    void testBuildOutputJson_noPairsSubmitted() {
        var av = createAppVersionDescriptor("42", "TestApp", "2.0");
        var result = AviatorSSCCorrelateHelper.buildOutputJson(av, null, 0, 0, List.of(), "SKIPPED");

        assertTrue(result.get("artifactId").isNull());
        assertEquals("SKIPPED", result.get(IActionCommandResultSupplier.actionFieldName).asText());

        JsonNode correlate = result.get("operation").get("correlate");
        assertTrue(correlate.get("message").isNull());
        assertTrue(correlate.get("submitted").isNull());
        assertTrue(correlate.get("succeeded").isNull());
        assertTrue(correlate.get("skipped").isNull());
        assertEquals(0, correlate.get("correlated").asInt());
    }

    // ── isVulnerabilitySuppressed ────────────────────────────────────────

    @Test
    void testIsVulnerabilitySuppressed_true() {
        var vuln = Vulnerability.builder().instanceID("INST-1").build();
        var auditIssue = AuditIssue.builder().instanceId("INST-1").suppressed(true).build();
        Map<String, AuditIssue> auditMap = Map.of("INST-1", auditIssue);

        assertTrue(AviatorSSCCorrelateHelper.isVulnerabilitySuppressed(vuln, auditMap));
    }

    @Test
    void testIsVulnerabilitySuppressed_false() {
        var vuln = Vulnerability.builder().instanceID("INST-1").build();
        var auditIssue = AuditIssue.builder().instanceId("INST-1").suppressed(false).build();
        Map<String, AuditIssue> auditMap = Map.of("INST-1", auditIssue);

        assertFalse(AviatorSSCCorrelateHelper.isVulnerabilitySuppressed(vuln, auditMap));
    }

    @Test
    void testIsVulnerabilitySuppressed_notInMap() {
        var vuln = Vulnerability.builder().instanceID("INST-999").build();
        Map<String, AuditIssue> auditMap = Map.of();

        assertFalse(AviatorSSCCorrelateHelper.isVulnerabilitySuppressed(vuln, auditMap));
    }

    @Test
    void testIsVulnerabilitySuppressed_nullMap() {
        var vuln = Vulnerability.builder().instanceID("INST-1").build();
        assertFalse(AviatorSSCCorrelateHelper.isVulnerabilitySuppressed(vuln, null));
    }

    @Test
    void testIsVulnerabilitySuppressed_nullInstanceId() {
        var vuln = Vulnerability.builder().instanceID(null).build();
        Map<String, AuditIssue> auditMap = new HashMap<>();
        assertFalse(AviatorSSCCorrelateHelper.isVulnerabilitySuppressed(vuln, auditMap));
    }

    // ── validateDownloadedFpr ────────────────────────────────────────────

    @Test
    void testValidateDownloadedFpr_validFile() throws IOException {
        Path fpr = tempDir.resolve("test.fpr");
        Files.writeString(fpr, "dummy content");
        AviatorSSCCorrelateHelper.validateDownloadedFpr(fpr, "SAST");
    }

    @Test
    void testValidateDownloadedFpr_nullPath() {
        var ex = assertThrows(FcliSimpleException.class,
            () -> AviatorSSCCorrelateHelper.validateDownloadedFpr(null, "SAST"));
        assertTrue(ex.getMessage().contains("null"));
    }

    @Test
    void testValidateDownloadedFpr_nonExistentFile() {
        Path nonExistent = tempDir.resolve("does-not-exist.fpr");
        var ex = assertThrows(FcliSimpleException.class,
            () -> AviatorSSCCorrelateHelper.validateDownloadedFpr(nonExistent, "DAST"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void testValidateDownloadedFpr_directory() throws IOException {
        Path dir = tempDir.resolve("adir");
        Files.createDirectory(dir);
        var ex = assertThrows(FcliSimpleException.class,
            () -> AviatorSSCCorrelateHelper.validateDownloadedFpr(dir, "SAST"));
        assertTrue(ex.getMessage().contains("not a regular file"));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private SSCAppVersionDescriptor createAppVersionDescriptor(String id, String appName, String versionName) {
        var descriptor = new SSCAppVersionDescriptor();
        descriptor.setVersionId(id);
        descriptor.setApplicationName(appName);
        descriptor.setVersionName(versionName);
        return descriptor;
    }
}
