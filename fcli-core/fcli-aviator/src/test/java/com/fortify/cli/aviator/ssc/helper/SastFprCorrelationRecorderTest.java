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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.aviator.util.FprHandle;

class SastFprCorrelationRecorderTest {

    private static final String SCAN_GUID = "test-scan-guid";

    @TempDir Path tempDir;

    // ── parseTagValue ────────────────────────────────────────────────────

    @Test
    void testParseTagValue_correlatedAndRejected() {
        var map = SastFprCorrelationRecorder.parseTagValue("CORRELATED::D1,D2|REJECTED::D3");
        assertEquals(3, map.size());
        assertEquals("CORRELATED", map.get("D1"));
        assertEquals("CORRELATED", map.get("D2"));
        assertEquals("REJECTED", map.get("D3"));
    }

    @Test
    void testParseTagValue_correlatedOnly() {
        var map = SastFprCorrelationRecorder.parseTagValue("CORRELATED::D1,D2");
        assertEquals(2, map.size());
        assertEquals("CORRELATED", map.get("D1"));
        assertEquals("CORRELATED", map.get("D2"));
    }

    @Test
    void testParseTagValue_rejectedOnly() {
        var map = SastFprCorrelationRecorder.parseTagValue("REJECTED::D3,D4");
        assertEquals(2, map.size());
        assertEquals("REJECTED", map.get("D3"));
        assertEquals("REJECTED", map.get("D4"));
    }

    @Test
    void testParseTagValue_null() {
        var map = SastFprCorrelationRecorder.parseTagValue(null);
        assertTrue(map.isEmpty());
    }

    @Test
    void testParseTagValue_empty() {
        var map = SastFprCorrelationRecorder.parseTagValue("");
        assertTrue(map.isEmpty());
    }

    @Test
    void testParseTagValue_malformedNoSeparator() {
        var map = SastFprCorrelationRecorder.parseTagValue("CORRELATED_D1");
        assertTrue(map.isEmpty(), "Malformed tag value without :: separator should produce empty map");
    }

    // ── buildTagValue ────────────────────────────────────────────────────

    @Test
    void testBuildTagValue_correlatedAndRejected() {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("D1", "CORRELATED");
        map.put("D2", "CORRELATED");
        map.put("D3", "REJECTED");

        String value = SastFprCorrelationRecorder.buildTagValue(map);
        assertEquals("CORRELATED::D1,D2|REJECTED::D3", value);
    }

    @Test
    void testBuildTagValue_correlatedOnly() {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("D1", "CORRELATED");

        String value = SastFprCorrelationRecorder.buildTagValue(map);
        assertEquals("CORRELATED::D1", value);
    }

    @Test
    void testBuildTagValue_emptyMap() {
        String value = SastFprCorrelationRecorder.buildTagValue(Map.of());
        assertEquals("", value);
    }

    // ── roundtrip ────────────────────────────────────────────────────────

    @Test
    void testRoundtrip_parseAndBuild() {
        String original = "CORRELATED::D1,D2|REJECTED::D3,D4";
        var parsed = SastFprCorrelationRecorder.parseTagValue(original);
        String rebuilt = SastFprCorrelationRecorder.buildTagValue(parsed);
        // Re-parse to check semantic equality (order may differ)
        var reParsed = SastFprCorrelationRecorder.parseTagValue(rebuilt);
        assertEquals(parsed, reParsed);
    }

    // ── writeCorrelationTags + readTriedPairKeys (integration) ───────────

    @Test
    void testWriteAndReadCorrelationTags() throws Exception {
        Path fprPath = createMinimalSastFpr();

        List<CorrelatedPair> confirmed = List.of(
            new CorrelatedPair("SAST-1", "DAST-A", SCAN_GUID, "HIGH", "match"),
            new CorrelatedPair("SAST-1", "DAST-B", SCAN_GUID, "MEDIUM", "match")
        );
        List<CorrelatedPair> rejected = List.of(
            new CorrelatedPair("SAST-1", "DAST-C", SCAN_GUID, "LOW", "no match"),
            new CorrelatedPair("SAST-2", "DAST-A", SCAN_GUID, "LOW", "no match")
        );

        SastFprCorrelationRecorder.writeCorrelationTags(fprPath, confirmed, rejected);

        Set<String> triedKeys = SastFprCorrelationRecorder.readTriedPairKeys(fprPath);
        assertEquals(4, triedKeys.size());
        assertTrue(triedKeys.contains("SAST-1::DAST-A"));
        assertTrue(triedKeys.contains("SAST-1::DAST-B"));
        assertTrue(triedKeys.contains("SAST-1::DAST-C"));
        assertTrue(triedKeys.contains("SAST-2::DAST-A"));
    }

    @Test
    void testWriteCorrelationTags_emptyLists() throws Exception {
        Path fprPath = createMinimalSastFpr();

        // Should not throw
        SastFprCorrelationRecorder.writeCorrelationTags(fprPath, List.of(), List.of());

        Set<String> triedKeys = SastFprCorrelationRecorder.readTriedPairKeys(fprPath);
        assertTrue(triedKeys.isEmpty());
    }

    @Test
    void testWriteCorrelationTags_mergePreservesCorrelated() throws Exception {
        Path fprPath = createMinimalSastFpr();

        // Run 1: SAST-1 ↔ DAST-A confirmed
        SastFprCorrelationRecorder.writeCorrelationTags(fprPath,
            List.of(new CorrelatedPair("SAST-1", "DAST-A", SCAN_GUID, "HIGH", "confirmed")),
            List.of());

        // Run 2: Try to reject SAST-1 ↔ DAST-A (should be ignored — CORRELATED is sticky)
        SastFprCorrelationRecorder.writeCorrelationTags(fprPath,
            List.of(),
            List.of(new CorrelatedPair("SAST-1", "DAST-A", SCAN_GUID, "LOW", "rejected")));

        // The pair should still be present in tried keys
        Set<String> triedKeys = SastFprCorrelationRecorder.readTriedPairKeys(fprPath);
        assertTrue(triedKeys.contains("SAST-1::DAST-A"));
    }

    @Test
    void testReadTriedPairKeys_noAuditXml() throws Exception {
        Path fprPath = createFprWithoutAuditXml();
        Set<String> keys = SastFprCorrelationRecorder.readTriedPairKeys(fprPath);
        assertTrue(keys.isEmpty());
    }

    @Test
    void testReadTriedPairKeys_doesNotResolveExternalEntity() throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "PRIVATE-SENTINEL");
        Path fprPath = createFprWithAuditXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE Audit [<!ENTITY xxe SYSTEM "%s">]>
            <Audit xmlns="xmlns://www.fortify.com/schema/audit">
              <IssueList><Issue instanceId="SAST-1"><Tag id="%s"><Value>CORRELATED::&xxe;</Value></Tag></Issue></IssueList>
            </Audit>
            """.formatted(secret.toUri(), SastFprCorrelationRecorder.DAST_CORRELATION_TAG_ID));

        assertTrue(SastFprCorrelationRecorder.readTriedPairKeys(fprPath).isEmpty());
        SastFprCorrelationRecorder.writeCorrelationTags(fprPath,
            List.of(new CorrelatedPair("SAST-1", "DAST-A", SCAN_GUID, "HIGH", "match")), List.of());
        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath)) {
            String output = Files.readString(zipFs.getPath("/audit.xml"));
            assertTrue(output.contains("CORRELATED::DAST-A"));
            assertTrue(!output.contains("PRIVATE-SENTINEL"));
        }
    }

    @Test
    void testReadTriedPairKeys_allowsHarmlessDoctype() throws Exception {
        Path fprPath = createFprWithAuditXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE Audit SYSTEM "http://example.invalid/audit.dtd">
            <Audit xmlns="xmlns://www.fortify.com/schema/audit">
              <IssueList><Issue instanceId="SAST-1"><Tag id="%s"><Value>CORRELATED::DAST-A</Value></Tag></Issue></IssueList>
            </Audit>
            """.formatted(SastFprCorrelationRecorder.DAST_CORRELATION_TAG_ID));

        try (FprHandle handle = new FprHandle(fprPath)) {
            Files.createDirectories(handle.getPath("/src-archive"));
            Files.writeString(handle.getPath("/src-archive/index.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
                <properties><entry key="Test.java">src-archive/Test.java</entry></properties>
                """);
            Files.writeString(handle.getPath("/src-archive/Test.java"), "class Test {}");
            assertEquals("src-archive/Test.java", handle.getSourceFileMap().get("Test.java"));
        }

        SastFprCorrelationRecorder.writeCorrelationTags(fprPath,
            List.of(new CorrelatedPair("SAST-1", "DAST-B", SCAN_GUID, "HIGH", "match")), List.of());
        assertEquals(Set.of("SAST-1::DAST-A", "SAST-1::DAST-B"), SastFprCorrelationRecorder.readTriedPairKeys(fprPath));
        try (FprHandle handle = new FprHandle(fprPath)) {
            assertEquals("src-archive/Test.java", handle.getSourceFileMap().get("Test.java"));
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Path createFprWithAuditXml(String auditXml) throws Exception {
        Path fprPath = tempDir.resolve("custom-audit.fpr");
        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, Map.of("create", "true"))) {
            Files.writeString(zipFs.getPath("/audit.xml"), auditXml);
        }
        return fprPath;
    }

    private Path createMinimalSastFpr() throws Exception {
        Path fprPath = tempDir.resolve("test-sast.fpr");
        if (Files.exists(fprPath)) {
            Files.delete(fprPath);
        }

        Map<String, String> zipProps = Map.of("create", "true");
        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, zipProps)) {
            Path auditXmlDest = zipFs.getPath("/audit.xml");
            String auditXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Audit xmlns="xmlns://www.fortify.com/schema/audit">
                  <ProjectInfo>
                    <Name>TestProject</Name>
                  </ProjectInfo>
                  <IssueList/>
                </Audit>
                """;
            Files.writeString(auditXmlDest, auditXml);
        }

        return fprPath;
    }

    private Path createFprWithoutAuditXml() throws Exception {
        Path fprPath = tempDir.resolve("no-audit.fpr");
        if (Files.exists(fprPath)) {
            Files.delete(fprPath);
        }

        Map<String, String> zipProps = Map.of("create", "true");
        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, zipProps)) {
            Path dummyFile = zipFs.getPath("/dummy.txt");
            Files.writeString(dummyFile, "placeholder");
        }

        return fprPath;
    }
}
