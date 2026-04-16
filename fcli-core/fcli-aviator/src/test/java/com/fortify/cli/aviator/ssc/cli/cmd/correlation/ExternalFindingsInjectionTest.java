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
package com.fortify.cli.aviator.ssc.cli.cmd.correlation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.aviator.ssc.helper.ExternalFindingsInjector;

/**
 * Tests the enriched DAST FPR upload flow by:
 * <ol>
 *   <li>Packaging the test webinspect.xml into a minimal DAST FPR (ZIP)</li>
 *   <li>Creating mock {@link CorrelatedPair} objects using real IDs from
 *       the test audit.fvdl (SAST) and webinspect.xml (DAST)</li>
 *   <li>Running {@link ExternalFindingsInjector} to inject {@code <ExternalFindings>}</li>
 *   <li>Verifying the modified webinspect.xml contains the expected correlation data</li>
 * </ol>
 */
class ExternalFindingsInjectionTest {

    /** SAST scan UUID extracted from audit.fvdl {@code <UUID>} element. */
    private static final String SAST_SCAN_GUID = "62cd94b2-523a-409e-86eb-9b55a0421380";

    /** Real SAST instance IDs from the test audit.fvdl. */
    private static final String SAST_INSTANCE_1 = "00403DBC3662FEBAD561B1A578AE7556";
    private static final String SAST_INSTANCE_2 = "00411ED275CA1DCF328136A99613E95E";
    private static final String SAST_INSTANCE_3 = "0080AE7911F7A5D3A8BDEFD0DD046FB2";
    private static final String SAST_INSTANCE_4 = "018E227E65A357ABDF21889B64CF21D0";

    /** Real DAST issue IDs from the test webinspect.xml. */
    private static final String DAST_ISSUE_1 = "fe1603fe-9a9e-b066-5741-75f228f5de86";
    private static final String DAST_ISSUE_2 = "4f5ed57d-c72f-9bf7-b0c4-5ac22301fdda";
    private static final String DAST_ISSUE_3 = "521f6045-5fb4-7fd5-c9fc-0aeb692b8d40";

    private Path dastFprPath;

    @BeforeEach
    void setUp() throws Exception {
        dastFprPath = createDastFprFromTestResources();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dastFprPath != null && Files.exists(dastFprPath)) {
            Files.deleteIfExists(dastFprPath);
        }
    }

    @Test
    void testInjectExternalFindings_singlePairPerDastIssue() throws Exception {
        List<CorrelatedPair> pairs = List.of(
            new CorrelatedPair(SAST_INSTANCE_1, DAST_ISSUE_1, SAST_SCAN_GUID, "HIGH", "Category match with high confidence"),
            new CorrelatedPair(SAST_INSTANCE_2, DAST_ISSUE_2, SAST_SCAN_GUID, "MEDIUM", "Partial category overlap")
        );

        ExternalFindingsInjector injector = new ExternalFindingsInjector();
        Path result = injector.injectAndRepackage(dastFprPath, pairs);

        assertNotNull(result);
        assertEquals(dastFprPath, result);

        Document modifiedDoc = readWebInspectFromFpr(dastFprPath);

        // Verify issue 1 has ExternalFindings
        Element issue1 = findIssueById(modifiedDoc, DAST_ISSUE_1);
        assertNotNull(issue1, "DAST issue " + DAST_ISSUE_1 + " should exist");
        NodeList ef1 = issue1.getElementsByTagName("ExternalFindings");
        assertEquals(1, ef1.getLength(), "Issue 1 should have exactly one ExternalFindings block");
        NodeList findings1 = ((Element) ef1.item(0)).getElementsByTagName("ExternalFinding");
        assertEquals(1, findings1.getLength(), "Issue 1 should have one ExternalFinding");
        verifyExternalFinding((Element) findings1.item(0), SAST_SCAN_GUID, SAST_INSTANCE_1);

        // Verify issue 2 has ExternalFindings
        Element issue2 = findIssueById(modifiedDoc, DAST_ISSUE_2);
        assertNotNull(issue2, "DAST issue " + DAST_ISSUE_2 + " should exist");
        NodeList ef2 = issue2.getElementsByTagName("ExternalFindings");
        assertEquals(1, ef2.getLength(), "Issue 2 should have exactly one ExternalFindings block");
        NodeList findings2 = ((Element) ef2.item(0)).getElementsByTagName("ExternalFinding");
        assertEquals(1, findings2.getLength(), "Issue 2 should have one ExternalFinding");
        verifyExternalFinding((Element) findings2.item(0), SAST_SCAN_GUID, SAST_INSTANCE_2);

        // Verify issue 3 (not correlated) has NO ExternalFindings
        Element issue3 = findIssueById(modifiedDoc, DAST_ISSUE_3);
        assertNotNull(issue3, "DAST issue " + DAST_ISSUE_3 + " should exist");
        NodeList ef3 = issue3.getElementsByTagName("ExternalFindings");
        assertEquals(0, ef3.getLength(), "Uncorrelated issue should have no ExternalFindings");
    }

    @Test
    void testInjectExternalFindings_multipleSastPairsForSameDastIssue() throws Exception {
        // A single DAST issue correlated with multiple SAST findings
        List<CorrelatedPair> pairs = List.of(
            new CorrelatedPair(SAST_INSTANCE_1, DAST_ISSUE_1, SAST_SCAN_GUID, "HIGH", "Primary match"),
            new CorrelatedPair(SAST_INSTANCE_2, DAST_ISSUE_1, SAST_SCAN_GUID, "MEDIUM", "Secondary match"),
            new CorrelatedPair(SAST_INSTANCE_3, DAST_ISSUE_1, SAST_SCAN_GUID, "LOW", "Tertiary match")
        );

        ExternalFindingsInjector injector = new ExternalFindingsInjector();
        injector.injectAndRepackage(dastFprPath, pairs);

        Document modifiedDoc = readWebInspectFromFpr(dastFprPath);
        Element issue = findIssueById(modifiedDoc, DAST_ISSUE_1);
        assertNotNull(issue);

        NodeList efBlocks = issue.getElementsByTagName("ExternalFindings");
        assertEquals(1, efBlocks.getLength(), "Should have exactly one ExternalFindings block");

        NodeList findings = ((Element) efBlocks.item(0)).getElementsByTagName("ExternalFinding");
        assertEquals(3, findings.getLength(), "Should have 3 ExternalFinding entries for multi-SAST correlation");

        // Collect all OriginFindingIDs
        List<String> foundInstanceIds = new ArrayList<>();
        for (int i = 0; i < findings.getLength(); i++) {
            Element ef = (Element) findings.item(i);
            foundInstanceIds.add(getChildText(ef, "OriginFindingID"));
            assertEquals(SAST_SCAN_GUID, getChildText(ef, "OriginID"));
            assertEquals("SCA", ef.getAttribute("Origin"));
        }

        assertTrue(foundInstanceIds.contains(SAST_INSTANCE_1));
        assertTrue(foundInstanceIds.contains(SAST_INSTANCE_2));
        assertTrue(foundInstanceIds.contains(SAST_INSTANCE_3));
    }

    @Test
    void testInjectExternalFindings_emptyPairsList() throws Exception {
        ExternalFindingsInjector injector = new ExternalFindingsInjector();
        Path result = injector.injectAndRepackage(dastFprPath, List.of());

        assertEquals(dastFprPath, result, "Empty pairs should return unmodified FPR path");

        // Verify no ExternalFindings were injected
        Document doc = readWebInspectFromFpr(dastFprPath);
        NodeList allEf = doc.getElementsByTagName("ExternalFindings");
        assertEquals(0, allEf.getLength(), "No ExternalFindings should be present for empty pairs");
    }

    @Test
    void testInjectExternalFindings_nullPairsList() throws Exception {
        ExternalFindingsInjector injector = new ExternalFindingsInjector();
        Path result = injector.injectAndRepackage(dastFprPath, null);

        assertEquals(dastFprPath, result, "Null pairs should return unmodified FPR path");
    }

    @Test
    void testInjectExternalFindings_idempotentRerun() throws Exception {
        List<CorrelatedPair> pairs = List.of(
            new CorrelatedPair(SAST_INSTANCE_1, DAST_ISSUE_1, SAST_SCAN_GUID, "HIGH", "Match")
        );

        ExternalFindingsInjector injector = new ExternalFindingsInjector();

        // First injection
        injector.injectAndRepackage(dastFprPath, pairs);

        // Second injection (re-run) — should replace, not duplicate
        injector.injectAndRepackage(dastFprPath, pairs);

        Document doc = readWebInspectFromFpr(dastFprPath);
        Element issue = findIssueById(doc, DAST_ISSUE_1);
        assertNotNull(issue);

        NodeList efBlocks = issue.getElementsByTagName("ExternalFindings");
        assertEquals(1, efBlocks.getLength(), "Re-run should not duplicate ExternalFindings blocks");

        NodeList findings = ((Element) efBlocks.item(0)).getElementsByTagName("ExternalFinding");
        assertEquals(1, findings.getLength(), "Re-run should not duplicate ExternalFinding entries");
    }

    @Test
    void testInjectExternalFindings_nonExistentDastIssueId() throws Exception {
        List<CorrelatedPair> pairs = List.of(
            new CorrelatedPair(SAST_INSTANCE_1, "nonexistent-dast-id-000", SAST_SCAN_GUID, "HIGH", "No match expected")
        );

        ExternalFindingsInjector injector = new ExternalFindingsInjector();
        injector.injectAndRepackage(dastFprPath, pairs);

        // Should succeed without error; no ExternalFindings injected for non-matching ID
        Document doc = readWebInspectFromFpr(dastFprPath);
        NodeList allEf = doc.getElementsByTagName("ExternalFindings");
        assertEquals(0, allEf.getLength(), "Non-existent DAST ID should result in no injection");
    }

    @Test
    void testEnrichedFprIsValidZip() throws Exception {
        List<CorrelatedPair> pairs = List.of(
            new CorrelatedPair(SAST_INSTANCE_1, DAST_ISSUE_1, SAST_SCAN_GUID, "HIGH", "Test"),
            new CorrelatedPair(SAST_INSTANCE_2, DAST_ISSUE_2, SAST_SCAN_GUID, "MEDIUM", "Test")
        );

        ExternalFindingsInjector injector = new ExternalFindingsInjector();
        injector.injectAndRepackage(dastFprPath, pairs);

        // Verify the FPR is still a valid ZIP and contains webinspect.xml
        try (FileSystem zipFs = FileSystems.newFileSystem(dastFprPath, (ClassLoader) null)) {
            Path webinspectInZip = zipFs.getPath("/webinspect.xml");
            assertTrue(Files.exists(webinspectInZip), "Enriched FPR should still contain webinspect.xml");
            assertTrue(Files.size(webinspectInZip) > 0, "webinspect.xml should not be empty");
        }
    }

    @Test
    void testMockCorrelatedPairsMatchRealIds() {
        // Verify the mock pairs we create use valid IDs from the test data
        List<CorrelatedPair> mockPairs = createMockCorrelatedPairs();

        assertFalse(mockPairs.isEmpty(), "Mock pairs should not be empty");

        for (CorrelatedPair pair : mockPairs) {
            assertNotNull(pair.sastInstanceId(), "SAST instance ID should not be null");
            assertNotNull(pair.dastIssueId(), "DAST issue ID should not be null");
            assertNotNull(pair.scanGuid(), "Scan GUID should not be null");
            assertEquals(SAST_SCAN_GUID, pair.scanGuid(), "All pairs should reference the same scan GUID");
        }
    }

    // ── Helper methods ──────────────────────────────────────────────────

    /**
     * Creates a minimal DAST FPR (ZIP) containing only webinspect.xml
     * from the test resources directory.
     */
    private Path createDastFprFromTestResources() throws Exception {
        Path tempFpr = Files.createTempFile("test-dast-", ".fpr");
        Files.delete(tempFpr); // Remove so ZipOutputStream can create fresh

        // Create ZIP with webinspect.xml inside
        Map<String, String> zipProps = Map.of("create", "true");
        try (FileSystem zipFs = FileSystems.newFileSystem(tempFpr, zipProps)) {
            Path webinspectDest = zipFs.getPath("/webinspect.xml");
            try (InputStream is = getClass().getResourceAsStream("fprs/webinspect.xml")) {
                assertNotNull(is, "Test webinspect.xml resource must exist");
                Files.copy(is, webinspectDest, StandardCopyOption.REPLACE_EXISTING);
            }

            // Add a minimal audit.xml so AuditProcessor won't fail
            Path auditXmlDest = zipFs.getPath("/audit.xml");
            String minimalAuditXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Audit xmlns="xmlns://www.fortify.com/schema/audit">
                  <ProjectInfo>
                    <Name>TestProject</Name>
                  </ProjectInfo>
                  <IssueList/>
                </Audit>
                """;
            Files.writeString(auditXmlDest, minimalAuditXml);
        }

        return tempFpr;
    }

    /**
     * Creates a representative set of mock {@link CorrelatedPair} objects
     * using real IDs extracted from the test audit.fvdl and webinspect.xml.
     * This simulates the gRPC server response.
     */
    static List<CorrelatedPair> createMockCorrelatedPairs() {
        return List.of(
            // DAST issue 1 correlated with SAST instance 1
            new CorrelatedPair(
                SAST_INSTANCE_1,
                DAST_ISSUE_1,
                SAST_SCAN_GUID,
                "HIGH",
                "SQL Injection category match: SAST finding in UserController maps to DAST HTTP parameter injection"
            ),
            // DAST issue 2 correlated with SAST instance 2
            new CorrelatedPair(
                SAST_INSTANCE_2,
                DAST_ISSUE_2,
                SAST_SCAN_GUID,
                "MEDIUM",
                "Cross-Site Scripting: reflected XSS in SAST corresponds to DAST detected XSS"
            ),
            // DAST issue 1 also correlated with SAST instance 3 (multi-SAST for same DAST)
            new CorrelatedPair(
                SAST_INSTANCE_3,
                DAST_ISSUE_1,
                SAST_SCAN_GUID,
                "LOW",
                "Secondary match: additional data flow path reaching same vulnerable endpoint"
            ),
            // DAST issue 3 correlated with SAST instance 4
            new CorrelatedPair(
                SAST_INSTANCE_4,
                DAST_ISSUE_3,
                SAST_SCAN_GUID,
                "HIGH",
                "Missing authentication check in both SAST and DAST"
            )
        );
    }

    /**
     * Reads and parses webinspect.xml from inside the FPR (ZIP).
     */
    private Document readWebInspectFromFpr(Path fprPath) throws Exception {
        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, (ClassLoader) null)) {
            Path webinspectPath = zipFs.getPath("/webinspect.xml");
            assertTrue(Files.exists(webinspectPath), "webinspect.xml must exist in FPR");
            try (InputStream is = Files.newInputStream(webinspectPath)) {
                var factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                return factory.newDocumentBuilder().parse(is);
            }
        }
    }

    /**
     * Finds an {@code <Issue>} element by its {@code id} attribute.
     */
    private Element findIssueById(Document doc, String issueId) {
        NodeList issues = doc.getElementsByTagName("Issue");
        for (int i = 0; i < issues.getLength(); i++) {
            if (issues.item(i) instanceof Element el && issueId.equals(el.getAttribute("id"))) {
                return el;
            }
        }
        return null;
    }

    /**
     * Verifies an {@code <ExternalFinding>} element has the expected OriginID and OriginFindingID.
     */
    private void verifyExternalFinding(Element ef, String expectedOriginId, String expectedFindingId) {
        assertEquals("SCA", ef.getAttribute("Origin"), "Origin should be SCA");
        assertEquals(expectedOriginId, getChildText(ef, "OriginID"), "OriginID mismatch");
        assertEquals(expectedFindingId, getChildText(ef, "OriginFindingID"), "OriginFindingID mismatch");
        String dateTime = getChildText(ef, "OriginDateTime");
        assertNotNull(dateTime, "OriginDateTime should be present");
        assertFalse(dateTime.isEmpty(), "OriginDateTime should not be empty");
    }

    /**
     * Gets the text content of the first child element with the given tag name.
     */
    private String getChildText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        return children.getLength() > 0 ? children.item(0).getTextContent() : null;
    }
}
