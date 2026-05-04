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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.dast.StreamingWebInspectParser;
import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.aviator.ssc.helper.DastFprCorrelationEnricher;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * Integration test that exercises the full enriched DAST FPR flow:
 * <ol>
 *   <li>Package test webinspect.xml into a DAST FPR</li>
 *   <li>Parse DAST issues from the FPR using {@link StreamingWebInspectParser}</li>
 *   <li>Create mock {@link CorrelatedPair} objects using <b>real</b> parsed DAST issue IDs</li>
 *   <li>Inject {@code <ExternalFindings>} via {@link DastFprCorrelationEnricher}</li>
 *   <li>Verify the enriched FPR is valid and ready for SSC upload (old artifact deletion + re-upload)</li>
 * </ol>
 */
class CorrelateAndUploadFlowTest {

    private static final String SAST_SCAN_GUID = "62cd94b2-523a-409e-86eb-9b55a0421380";
    private static final String SAST_INSTANCE_1 = "00403DBC3662FEBAD561B1A578AE7556";
    private static final String SAST_INSTANCE_2 = "00411ED275CA1DCF328136A99613E95E";
    private static final String SAST_INSTANCE_3 = "0080AE7911F7A5D3A8BDEFD0DD046FB2";

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
    void testFullCorrelationFlow_parseInjectAndVerify() throws Exception {
        // Step 1: Parse DAST issues from the FPR
        List<DastIssue> dastIssues;
        try (FprHandle fprHandle = new FprHandle(dastFprPath)) {
            StreamingWebInspectParser parser = new StreamingWebInspectParser(fprHandle);
            dastIssues = parser.parse();
        }

        assertFalse(dastIssues.isEmpty(), "DAST FPR should contain issues");
        assertTrue(dastIssues.size() > 3, "Expected more than 3 DAST issues; got " + dastIssues.size());

        // Step 2: Build mock correlated pairs using real parsed DAST issue IDs
        String realDastId1 = dastIssues.get(0).getId();
        String realDastId2 = dastIssues.get(1).getId();
        String realDastId3 = dastIssues.get(2).getId();

        assertNotNull(realDastId1, "Parsed DAST issue should have an ID");
        assertNotNull(realDastId2, "Parsed DAST issue should have an ID");

        List<CorrelatedPair> mockPairs = List.of(
            new CorrelatedPair(SAST_INSTANCE_1, realDastId1, SAST_SCAN_GUID, "HIGH",
                "Category match: " + dastIssues.get(0).getCategory()),
            new CorrelatedPair(SAST_INSTANCE_2, realDastId2, SAST_SCAN_GUID, "MEDIUM",
                "Category match: " + dastIssues.get(1).getCategory()),
            new CorrelatedPair(SAST_INSTANCE_3, realDastId1, SAST_SCAN_GUID, "LOW",
                "Secondary match for same DAST issue")
        );

        // Step 3: Inject ExternalFindings
        DastFprCorrelationEnricher injector = new DastFprCorrelationEnricher();
        Path enrichedFpr = injector.injectAndRepackage(dastFprPath, mockPairs);

        assertNotNull(enrichedFpr);
        assertTrue(Files.exists(enrichedFpr), "Enriched FPR file should exist");
        assertTrue(Files.size(enrichedFpr) > 0, "Enriched FPR should not be empty");

        // Step 4: Verify injection results
        Document modifiedDoc = readWebInspectFromFpr(enrichedFpr);

        // Issue 1: should have 2 ExternalFinding entries (SAST_INSTANCE_1 + SAST_INSTANCE_3)
        Element issue1 = findIssueById(modifiedDoc, realDastId1);
        assertNotNull(issue1, "First DAST issue should exist after injection");
        NodeList ef1 = issue1.getElementsByTagName("ExternalFindings");
        assertEquals(1, ef1.getLength());
        NodeList findings1 = ((Element) ef1.item(0)).getElementsByTagName("ExternalFinding");
        assertEquals(2, findings1.getLength(), "Issue 1 should have 2 correlated SAST findings");

        Set<String> issue1SastIds = extractOriginFindingIds(findings1);
        assertTrue(issue1SastIds.contains(SAST_INSTANCE_1));
        assertTrue(issue1SastIds.contains(SAST_INSTANCE_3));

        // Issue 2: should have 1 ExternalFinding entry
        Element issue2 = findIssueById(modifiedDoc, realDastId2);
        assertNotNull(issue2);
        NodeList ef2 = issue2.getElementsByTagName("ExternalFindings");
        assertEquals(1, ef2.getLength());
        NodeList findings2 = ((Element) ef2.item(0)).getElementsByTagName("ExternalFinding");
        assertEquals(1, findings2.getLength());

        // Issue 3: should have NO ExternalFindings (not correlated)
        Element issue3 = findIssueById(modifiedDoc, realDastId3);
        assertNotNull(issue3);
        NodeList ef3 = issue3.getElementsByTagName("ExternalFindings");
        assertEquals(0, ef3.getLength(), "Uncorrelated issue should remain unchanged");

        // Step 5: Verify enriched FPR is a valid ZIP (ready for SSC upload)
        verifyFprIsValidZip(enrichedFpr);
    }

    @Test
    void testEnrichedFprPreservesAllOriginalIssues() throws Exception {
        // Count original issues
        int originalIssueCount;
        try (FprHandle fprHandle = new FprHandle(dastFprPath)) {
            StreamingWebInspectParser parser = new StreamingWebInspectParser(fprHandle);
            originalIssueCount = parser.parse().size();
        }

        // Inject correlations
        List<CorrelatedPair> pairs = List.of(
            new CorrelatedPair(SAST_INSTANCE_1, "fe1603fe-9a9e-b066-5741-75f228f5de86",
                SAST_SCAN_GUID, "HIGH", "Test")
        );
        new DastFprCorrelationEnricher().injectAndRepackage(dastFprPath, pairs);

        // Verify all original issues are preserved
        Document doc = readWebInspectFromFpr(dastFprPath);
        NodeList allIssues = doc.getElementsByTagName("Issue");
        assertEquals(originalIssueCount, allIssues.getLength(),
            "Enrichment should not add or remove any Issue elements");
    }

    @Test
    void testDeleteAndReuploadReadiness() throws Exception {
        List<CorrelatedPair> pairs = ExternalFindingsInjectionTest.createMockCorrelatedPairs();

        DastFprCorrelationEnricher injector = new DastFprCorrelationEnricher();
        Path enrichedFpr = injector.injectAndRepackage(dastFprPath, pairs);

        // Simulate the delete+upload flow validation:
        // 1. The enriched FPR must exist and be a regular file
        assertTrue(Files.exists(enrichedFpr), "Enriched FPR must exist for upload");
        assertTrue(Files.isRegularFile(enrichedFpr), "Enriched FPR must be a regular file");

        // 2. The FPR must be larger than 0 bytes
        long fileSize = Files.size(enrichedFpr);
        assertTrue(fileSize > 0, "Enriched FPR must have content (size=" + fileSize + ")");

        // 3. It must be a valid ZIP containing webinspect.xml
        verifyFprIsValidZip(enrichedFpr);

        // 4. The webinspect.xml must contain ExternalFindings (correlation data)
        Document doc = readWebInspectFromFpr(enrichedFpr);
        NodeList efElements = doc.getElementsByTagName("ExternalFindings");
        assertTrue(efElements.getLength() > 0,
            "Enriched FPR must contain ExternalFindings for SSC correlation visibility");

        // At this point the flow would be:
        //   a) SSCArtifactHelper.delete(unirest, originalDastArtifactDescriptor)
        //   b) SSCFileTransferHelper.htmlUpload(unirest, url, enrichedFpr.toFile(), ...)
        // Both require a live SSC connection, so we validate readiness only.
    }

    // ── Helper methods ──────────────────────────────────────────────────

    private Path createDastFprFromTestResources() throws Exception {
        Path tempFpr = Files.createTempFile("test-dast-flow-", ".fpr");
        Files.delete(tempFpr);

        Map<String, String> zipProps = Map.of("create", "true");
        try (FileSystem zipFs = FileSystems.newFileSystem(tempFpr, zipProps)) {
            Path webinspectDest = zipFs.getPath("/webinspect.xml");
            try (InputStream is = getClass().getResourceAsStream("fprs/webinspect.xml")) {
                assertNotNull(is, "Test webinspect.xml resource must exist");
                Files.copy(is, webinspectDest, StandardCopyOption.REPLACE_EXISTING);
            }

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

    private Document readWebInspectFromFpr(Path fprPath) throws Exception {
        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, (ClassLoader) null)) {
            Path webinspectPath = zipFs.getPath("/webinspect.xml");
            try (InputStream is = Files.newInputStream(webinspectPath)) {
                var factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                return factory.newDocumentBuilder().parse(is);
            }
        }
    }

    private Element findIssueById(Document doc, String issueId) {
        NodeList issues = doc.getElementsByTagName("Issue");
        for (int i = 0; i < issues.getLength(); i++) {
            if (issues.item(i) instanceof Element el && issueId.equals(el.getAttribute("id"))) {
                return el;
            }
        }
        return null;
    }

    private Set<String> extractOriginFindingIds(NodeList findings) {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < findings.getLength(); i++) {
            Element ef = (Element) findings.item(i);
            NodeList originIds = ef.getElementsByTagName("OriginFindingID");
            if (originIds.getLength() > 0) {
                ids.add(originIds.item(0).getTextContent());
            }
        }
        return ids;
    }

    private void verifyFprIsValidZip(Path fprPath) throws Exception {
        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, (ClassLoader) null)) {
            Path webinspectInZip = zipFs.getPath("/webinspect.xml");
            assertTrue(Files.exists(webinspectInZip), "Enriched FPR should contain webinspect.xml");
            assertTrue(Files.size(webinspectInZip) > 0, "webinspect.xml should not be empty");
        }
    }
}
