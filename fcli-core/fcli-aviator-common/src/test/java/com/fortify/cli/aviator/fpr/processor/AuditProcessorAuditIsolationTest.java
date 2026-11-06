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
package com.fortify.cli.aviator.fpr.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;
import com.fortify.cli.aviator.audit.model.Autoremediation;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * Verifies that {@link AuditProcessor#updateAndSaveAuditAndRemediationsXml} prunes
 * {@code audit.xml} Issue entries that Aviator did not modify, so SSC upload isolation
 * preserves concurrent tag edits on Non-SAST / non-audited SAST findings.
 */
class AuditProcessorAuditIsolationTest {
    private static final String AUDIT_NS = "xmlns://www.fortify.com/schema/audit";

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
    void testPrunesUntouchedIssuesAndKeepsAuditedIssueState() throws Exception {
        createTestFpr(multiIssueAuditXml("instance-A", "instance-B", "instance-C"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-A", successResponse("instance-A")),
                createTagMappingConfig(),
                Map.of("instance-A", "Cross-Site Scripting"),
                new FPRInfo(fprHandle));

        Set<String> retainedIds = readIssueInstanceIds();
        assertEquals(Set.of("instance-A"), retainedIds);

        Element audited = readIssueElement("instance-A");
        assertEquals("1", audited.getAttribute("revision"));
        assertTrue(readTagValues(audited).contains(Constants.PROCESSED_BY_AVIATOR));
        assertTrue(readTagValues(audited).contains(Constants.NOT_AN_ISSUE)
                || readTagValues(audited).contains(Constants.AVIATOR_NOT_AN_ISSUE));
        assertTrue(readComments(audited).stream().anyMatch(c -> c.contains("Reviewed by Aviator")));
        assertTrue(hasTagHistory(audited));
    }

    @Test
    void testCoveredIssueWhoseFixIsNotWrittenIsNotWrittenToRemediationsXml() throws Exception {
        createTestFpr(multiIssueAuditXml("instance-B"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-B", coveredResponse("instance-B", List.of("instance-A"))),
                createTagMappingConfig(),
                Map.of("instance-B", "SQL Injection"),
                new FPRInfo(fprHandle));

        // instance-A has no entry of its own, so the covered issue would reference nothing and is not written.
        assertTrue(!Files.exists(fprHandle.getPath("/remediations.xml")));
    }

    @Test
    void testSupersededAndCoveredIssuesAreWrittenAsReferencesToTheSupersedingFix() throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A", "instance-D", "instance-C"),
                "Example.java", "line1\nline2\nline3\nline4\n");
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Map<String, AuditResponse> responses = new java.util.LinkedHashMap<>();
        responses.put("instance-D", fixResponse("instance-D", 2, 2, "D-CODE", List.of()));
        responses.put("instance-A", fixResponse("instance-A", 1, 3, "A-CODE", List.of("instance-D")));
        responses.put("instance-C", coveredResponse("instance-C", List.of("instance-D")));

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                responses,
                createTagMappingConfig(),
                Map.of("instance-A", "SQL Injection", "instance-D", "SQL Injection", "instance-C", "SQL Injection"),
                new FPRInfo(fprHandle));

        Map<String, Element> entries = readRemediationEntries();
        assertEquals(Set.of("instance-A", "instance-D", "instance-C"), entries.keySet());

        Element fix = entries.get("instance-A");
        assertEquals(1, fix.getElementsByTagNameNS(REMEDIATIONS_NS, "FileChanges").getLength());
        assertEquals(List.of("instance-D"), referencedIds(fix, "SupersedesPriorFix"));
        assertEquals(List.of(), referencedIds(fix, "CoveredByPriorFix"));

        // The superseded issue keeps no fix of its own, only a reference to the superseding fix.
        Element superseded = entries.get("instance-D");
        assertEquals(0, superseded.getElementsByTagNameNS(REMEDIATIONS_NS, "FileChanges").getLength());
        assertEquals(List.of("instance-A"), referencedIds(superseded, "CoveredByPriorFix"));

        // A reference to a superseded issue is redirected to the fix that superseded it.
        Element covered = entries.get("instance-C");
        assertEquals(0, covered.getElementsByTagNameNS(REMEDIATIONS_NS, "FileChanges").getLength());
        assertEquals(List.of("instance-A"), referencedIds(covered, "CoveredByPriorFix"));
    }

    @Test
    void testSupersededIssueIsResolvedToTheWidestFixInAChain() {
        Map<String, AuditResponse> responses = new java.util.LinkedHashMap<>();
        responses.put("D", fixResponse("D", 2, 2, "d", List.of()));
        responses.put("A", fixResponse("A", 1, 3, "a", List.of("D")));
        responses.put("E", fixResponse("E", 1, 5, "e", List.of("A")));
        Map<String, String> timestamps = Map.of("D", "t", "A", "t", "E", "t");

        Map<String, String> resolved = AuditProcessor.resolveSupersededByFix(responses, timestamps::containsKey);

        assertEquals(Map.of("D", "E", "A", "E"), resolved);
    }

    @Test
    void testSupersedeCyclesAndUnknownIssuesAreIgnored() {
        Map<String, AuditResponse> responses = new java.util.LinkedHashMap<>();
        responses.put("A", fixResponse("A", 1, 3, "a", List.of("B", "MISSING")));
        responses.put("B", fixResponse("B", 1, 3, "b", List.of("A")));
        Map<String, String> timestamps = Map.of("A", "t", "B", "t");

        assertEquals(Map.of(), AuditProcessor.resolveSupersededByFix(responses, timestamps::containsKey));
    }

    @Test
    void testCoveredNoteNamesTheFixThatSupersededTheCoveringFix() {
        Map<String, AuditResponse> responses = new java.util.LinkedHashMap<>();
        responses.put("A", fixResponse("A", 1, 3, "a", List.of()));
        responses.put("B", fixResponse("B", 1, 3, "b", List.of("A")));
        responses.put("C", coveredResponse("C", List.of("A")));
        responses.get("C").getAuditResult().setComment("Text. Note: No separate automated remediation was generated "
                + "for this finding because it is already addressed by the fix for issue A.");
        responses.put("E", coveredResponse("E", List.of("A", "B")));
        responses.get("E").getAuditResult().setComment("Note: It is already addressed by the fixes for issues A, B.");
        responses.put("F", coveredResponse("F", List.of("X")));
        responses.get("F").getAuditResult().setComment("Note: It is already addressed by the fix for issue X.");

        AuditProcessor.resolvePriorFixNotes(responses);

        assertEquals("Text. Note: No separate automated remediation was generated for this finding because it is "
                + "already addressed by the fix for issue B.", responses.get("C").getAuditResult().getComment());
        assertEquals("Note: It is already addressed by the fix for issue B.", responses.get("E").getAuditResult().getComment());
        assertEquals("Note: It is already addressed by the fix for issue X.", responses.get("F").getAuditResult().getComment());
    }

    @Test
    void testSupersedeNoteListsEveryAbsorbedIssueIncludingChains() {
        Map<String, AuditResponse> responses = new java.util.LinkedHashMap<>();
        responses.put("D", fixResponse("D", 2, 2, "d", List.of()));
        responses.put("A", fixResponse("A", 1, 3, "a", List.of("D")));
        responses.put("E", fixResponse("E", 1, 5, "e", List.of("A")));
        responses.get("E").getAuditResult().setComment("Note: The automated remediation for this finding also contains "
                + "the fix for issue A, which falls within the lines it changes.");

        AuditProcessor.resolvePriorFixNotes(responses);

        assertEquals("Note: The automated remediation for this finding also contains the fixes for issues A, D, "
                + "which falls within the lines it changes.", responses.get("E").getAuditResult().getComment());
    }

    @Test
    void testNotesAreLeftAloneWhenNothingWasSuperseded() {
        Map<String, AuditResponse> responses = new java.util.LinkedHashMap<>();
        responses.put("A", fixResponse("A", 1, 3, "a", List.of()));
        responses.put("C", coveredResponse("C", List.of("A")));
        responses.get("C").getAuditResult().setComment("Note: It is already addressed by the fix for issue A.");

        AuditProcessor.resolvePriorFixNotes(responses);

        assertEquals("Note: It is already addressed by the fix for issue A.", responses.get("C").getAuditResult().getComment());
    }

    @Test
    void testNarrowerFixIsKeptWhenTheWiderFixCannotBeWritten() throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A", "instance-D", "instance-C"),
                "Example.java", "line1\nline2\nline3\nline4\n");
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Map<String, AuditResponse> responses = new java.util.LinkedHashMap<>();
        responses.put("instance-D", fixResponse("instance-D", 2, 2, "D-CODE", List.of()));
        // The wider fix is for a file that is not in the source archive, so it cannot be written.
        responses.put("instance-A", fixResponse("instance-A", "Missing.java", 1, 3, "A-CODE", List.of("instance-D")));
        responses.put("instance-C", coveredResponse("instance-C", List.of("instance-D")));

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                responses,
                createTagMappingConfig(),
                Map.of("instance-A", "SQL Injection", "instance-D", "SQL Injection", "instance-C", "SQL Injection"),
                new FPRInfo(fprHandle));

        Map<String, Element> entries = readRemediationEntries();
        assertEquals(Set.of("instance-D", "instance-C"), entries.keySet());
        Element narrower = entries.get("instance-D");
        assertEquals(1, narrower.getElementsByTagNameNS(REMEDIATIONS_NS, "FileChanges").getLength());
        assertEquals(List.of(), referencedIds(narrower, "CoveredByPriorFix"));
        assertEquals(List.of(), referencedIds(narrower, "SupersedesPriorFix"));
        assertEquals(List.of("instance-D"), referencedIds(entries.get("instance-C"), "CoveredByPriorFix"));
    }

    @Test
    void testSupersededChainFallsBackToTheNearestWrittenFix() throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A", "instance-D", "instance-E"),
                "Example.java", "line1\nline2\nline3\nline4\n");
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Map<String, AuditResponse> responses = new java.util.LinkedHashMap<>();
        responses.put("instance-D", fixResponse("instance-D", 2, 2, "D-CODE", List.of()));
        responses.put("instance-A", fixResponse("instance-A", 1, 3, "A-CODE", List.of("instance-D")));
        // The widest fix cannot be written, so A stays a fix and D references A instead of E.
        responses.put("instance-E", fixResponse("instance-E", "Missing.java", 1, 4, "E-CODE", List.of("instance-A")));

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                responses,
                createTagMappingConfig(),
                Map.of("instance-A", "SQL Injection", "instance-D", "SQL Injection", "instance-E", "SQL Injection"),
                new FPRInfo(fprHandle));

        Map<String, Element> entries = readRemediationEntries();
        assertEquals(Set.of("instance-A", "instance-D"), entries.keySet());
        Element fix = entries.get("instance-A");
        assertEquals(1, fix.getElementsByTagNameNS(REMEDIATIONS_NS, "FileChanges").getLength());
        assertEquals(List.of("instance-D"), referencedIds(fix, "SupersedesPriorFix"));
        Element narrower = entries.get("instance-D");
        assertEquals(0, narrower.getElementsByTagNameNS(REMEDIATIONS_NS, "FileChanges").getLength());
        assertEquals(List.of("instance-A"), referencedIds(narrower, "CoveredByPriorFix"));
    }

    private static final String REMEDIATIONS_NS = "xmlns://www.fortify.com/schema/remediations";

    private static AuditResponse coveredResponse(String issueId, List<String> coveredBy) {
        return AuditResponse.builder()
                .issueId(issueId)
                .status("SUCCESS")
                .tier("GOLD")
                .auditResult(AuditResult.builder()
                        .tagValue(Constants.NOT_AN_ISSUE)
                        .comment("Covered by the fix for " + coveredBy)
                        .autoremediation(Autoremediation.builder()
                                .skipReason("Covered by prior fix")
                                .coveredByIssueIds(coveredBy)
                                .build())
                        .build())
                .build();
    }

    private static AuditResponse fixResponse(String issueId, int from, int to, String newCode, List<String> supersedes) {
        return fixResponse(issueId, "Example.java", from, to, newCode, supersedes);
    }

    private static AuditResponse fixResponse(String issueId, String file, int from, int to, String newCode,
            List<String> supersedes) {
        return AuditResponse.builder()
                .issueId(issueId)
                .status("SUCCESS")
                .tier("GOLD")
                .auditResult(AuditResult.builder()
                        .tagValue(Constants.NOT_AN_ISSUE)
                        .comment("Fix for " + issueId)
                        .autoremediation(Autoremediation.builder()
                                .changes(List.of(com.fortify.cli.aviator.audit.model.Change.builder()
                                        .file(file)
                                        .fromLine(String.valueOf(from))
                                        .toLine(String.valueOf(to))
                                        .replaceWith(newCode)
                                        .build()))
                                .supersededIssueIds(supersedes)
                                .build())
                        .build())
                .build();
    }

    private Map<String, Element> readRemediationEntries() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document remediations;
        try (var inputStream = Files.newInputStream(fprHandle.getPath("/remediations.xml"))) {
            remediations = factory.newDocumentBuilder().parse(inputStream);
        }
        Map<String, Element> entries = new java.util.LinkedHashMap<>();
        NodeList nodes = remediations.getElementsByTagNameNS(REMEDIATIONS_NS, "Remediation");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element entry = (Element) nodes.item(i);
            entries.put(entry.getAttribute("instanceId"), entry);
        }
        return entries;
    }

    private static List<String> referencedIds(Element entry, String elementName) {
        List<String> ids = new ArrayList<>();
        NodeList nodes = entry.getElementsByTagNameNS(REMEDIATIONS_NS, elementName);
        for (int i = 0; i < nodes.getLength(); i++) {
            ids.add(((Element) nodes.item(i)).getAttribute("instanceId"));
        }
        return ids;
    }

    private void createTestFprWithSource(String auditXml, String fileName, String content) throws Exception {
        tempFprFile = Files.createTempFile("audit-processor-isolation", ".fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {
            zipOutputStream.putNextEntry(new ZipEntry("audit.fvdl"));
            zipOutputStream.write(minimalAuditFvdl().getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("audit.xml"));
            zipOutputStream.write(auditXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zipOutputStream.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index><entry key=\"" + fileName
                    + "\">src-archive/" + fileName + "</entry></index>").getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("src-archive/" + fileName));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        fprHandle = new FprHandle(tempFprFile);
    }

    @Test
    void doesNotResolveExternalEntityInAuditXml() throws Exception {
        Path secret = Files.createTempFile("audit-xxe-sentinel", ".txt");
        try {
            Files.writeString(secret, "PRIVATE-SENTINEL");
            createTestFpr("""
                <!DOCTYPE Audit [<!ENTITY xxe SYSTEM "%s">]>
                <Audit xmlns="xmlns://www.fortify.com/schema/audit">
                                    <IssueList><Issue instanceId="issue-1"><Comment><Content>&xxe;</Content></Comment></Issue></IssueList>
                </Audit>
                """.formatted(secret.toUri()));

                        var issue = new AuditProcessor(fprHandle).processAuditXML().get("issue-1");
                        assertEquals("", issue.getThreadedComments().get(0).getContent());
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    void testPartialSuccessRetainsOnlySuccessfulIssue() throws Exception {
        createTestFpr(multiIssueAuditXml("instance-A", "instance-D"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Map<String, AuditResponse> responses = new HashMap<>();
        responses.put("instance-A", successResponse("instance-A"));
        responses.put("instance-D", failedResponse("instance-D"));

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                responses,
                createTagMappingConfig(),
                Map.of("instance-A", "SQL Injection"),
                new FPRInfo(fprHandle));

        assertEquals(Set.of("instance-A"), readIssueInstanceIds());
    }

    @Test
    void testNewIssueIsRetainedAndPreExistingUntouchedArePruned() throws Exception {
        createTestFpr(multiIssueAuditXml("instance-old-1", "instance-old-2"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-new", successResponse("instance-new")),
                createTagMappingConfig(),
                Map.of("instance-new", "Path Manipulation"),
                new FPRInfo(fprHandle));

        assertEquals(Set.of("instance-new"), readIssueInstanceIds());
    }

    @Test
    void testSilentSkippedDoesNotRetainUntouchedDownloadIssue() throws Exception {
        createTestFpr(multiIssueAuditXml("instance-A", "instance-B"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Map<String, AuditResponse> responses = new HashMap<>();
        responses.put("instance-A", successResponse("instance-A"));
        responses.put("instance-B", silentSkippedResponse("instance-B"));

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                responses,
                createTagMappingConfig(),
                Map.of("instance-A", "SQL Injection"),
                new FPRInfo(fprHandle));

        assertEquals(Set.of("instance-A"), readIssueInstanceIds());
    }

    @Test
    void testBlankInstanceIdIssueIsPruned() throws Exception {
        createTestFpr(auditXmlWithBlankAndNamedIssue("instance-A"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-A", successResponse("instance-A")),
                createTagMappingConfig(),
                Map.of("instance-A", "SQL Injection"),
                new FPRInfo(fprHandle));

        assertEquals(Set.of("instance-A"), readIssueInstanceIds());
    }

    @Test
    void testAllDownloadedIssuesAuditedResultsInZeroPrune() throws Exception {
        createTestFpr(multiIssueAuditXml("instance-A", "instance-B"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Map<String, AuditResponse> responses = new HashMap<>();
        responses.put("instance-A", successResponse("instance-A"));
        responses.put("instance-B", successResponse("instance-B"));

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                responses,
                createTagMappingConfig(),
                Map.of(
                        "instance-A", "SQL Injection",
                        "instance-B", "XSS"),
                new FPRInfo(fprHandle));

        assertEquals(Set.of("instance-A", "instance-B"), readIssueInstanceIds());
    }

    @Test
    void testLargeAuditXmlPruneIsPerformantAndCorrect() throws Exception {
        int totalIssues = 10_000;
        StringBuilder issues = new StringBuilder();
        for (int i = 0; i < totalIssues; i++) {
            issues.append("    <ns2:Issue instanceId=\"issue-").append(i)
                    .append("\" revision=\"0\" suppressed=\"false\"/>\n");
        }
        String auditXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="%s" version="4.4">
                  <ns2:IssueList>
                %s  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(AUDIT_NS, issues);

        createTestFpr(auditXml);
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        assertTimeoutPreemptively(Duration.ofSeconds(15), () ->
                auditProcessor.updateAndSaveAuditAndRemediationsXml(
                        Map.of("issue-42", successResponse("issue-42")),
                        createTagMappingConfig(),
                        Map.of("issue-42", "SQL Injection"),
                        new FPRInfo(fprHandle)));

        assertEquals(Set.of("issue-42"), readIssueInstanceIds());
    }

    private void createTestFpr(String auditXml) throws Exception {
        tempFprFile = Files.createTempFile("audit-processor-isolation", ".fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {
            zipOutputStream.putNextEntry(new ZipEntry("audit.fvdl"));
            zipOutputStream.write(minimalAuditFvdl().getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("audit.xml"));
            zipOutputStream.write(auditXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zipOutputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index/>".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        fprHandle = new FprHandle(tempFprFile);
    }

    private Document readAuditDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (var inputStream = Files.newInputStream(fprHandle.getPath("/audit.xml"))) {
            return factory.newDocumentBuilder().parse(inputStream);
        }
    }

    private Set<String> readIssueInstanceIds() throws Exception {
        Document document = readAuditDocument();
        NodeList nodes = document.getElementsByTagNameNS(AUDIT_NS, "Issue");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            ids.add(((Element) nodes.item(i)).getAttribute("instanceId"));
        }
        return ids;
    }

    private Element readIssueElement(String instanceId) throws Exception {
        Document document = readAuditDocument();
        NodeList nodes = document.getElementsByTagNameNS(AUDIT_NS, "Issue");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element issue = (Element) nodes.item(i);
            if (instanceId.equals(issue.getAttribute("instanceId"))) {
                return issue;
            }
        }
        throw new AssertionError("Issue not found: " + instanceId);
    }

    private List<String> readTagValues(Element issueElement) {
        List<String> values = new ArrayList<>();
        NodeList tags = issueElement.getElementsByTagNameNS(AUDIT_NS, "Tag");
        for (int i = 0; i < tags.getLength(); i++) {
            Element tag = (Element) tags.item(i);
            NodeList valueNodes = tag.getElementsByTagNameNS(AUDIT_NS, "Value");
            if (valueNodes.getLength() > 0) {
                values.add(valueNodes.item(0).getTextContent());
            }
        }
        return values;
    }

    private List<String> readComments(Element issueElement) {
        List<String> comments = new ArrayList<>();
        NodeList contentNodes = issueElement.getElementsByTagNameNS(AUDIT_NS, "Content");
        for (int i = 0; i < contentNodes.getLength(); i++) {
            comments.add(contentNodes.item(i).getTextContent());
        }
        return comments;
    }

    private boolean hasTagHistory(Element issueElement) {
        return issueElement.getElementsByTagNameNS(AUDIT_NS, "TagHistory").getLength() > 0;
    }

    private AuditResponse successResponse(String instanceId) {
        return AuditResponse.builder()
                .issueId(instanceId)
                .status("SUCCESS")
                .tier("GOLD")
                .auditResult(AuditResult.builder()
                        .tagValue(Constants.NOT_AN_ISSUE)
                        .comment("Reviewed by Aviator")
                        .build())
                .build();
    }

    private AuditResponse failedResponse(String instanceId) {
        return AuditResponse.builder()
                .issueId(instanceId)
                .status("FAILED")
                .statusMessage("backend error")
                .build();
    }

    private AuditResponse silentSkippedResponse(String instanceId) {
        return AuditResponse.builder()
                .issueId(instanceId)
                .status("SKIPPED")
                .auditResult(AuditResult.builder()
                        .tagValue(null)
                        .comment("")
                        .build())
                .build();
    }

    private TagMappingConfig createTagMappingConfig() {
        TagMappingConfig config = new TagMappingConfig();
        TagMappingConfig.Mapping mapping = new TagMappingConfig.Mapping();
        mapping.setTier_1(createTier(true));
        mapping.setTier_2(createTier(false));
        config.setMapping(mapping);
        config.validate();
        return config;
    }

    private TagMappingConfig.Tier createTier(boolean suppressFalsePositives) {
        TagMappingConfig.Tier tier = new TagMappingConfig.Tier();
        tier.setFp(createResult("Not an Issue", suppressFalsePositives));
        tier.setTp(createResult("Exploitable", false));
        tier.setUnsure(createResult(null, false));
        return tier;
    }

    private TagMappingConfig.Result createResult(String value, boolean suppress) {
        TagMappingConfig.Result result = new TagMappingConfig.Result();
        result.setValue(value);
        result.setSuppress(suppress);
        return result;
    }

    private String multiIssueAuditXml(String... instanceIds) {
        StringBuilder issues = new StringBuilder();
        for (String id : instanceIds) {
            issues.append("    <ns2:Issue instanceId=\"").append(id)
                    .append("\" revision=\"0\" suppressed=\"false\"/>\n");
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="%s" version="4.4">
                  <ns2:IssueList>
                %s  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(AUDIT_NS, issues);
    }

    private String auditXmlWithBlankAndNamedIssue(String namedId) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="%s" version="4.4">
                  <ns2:IssueList>
                    <ns2:Issue instanceId="" revision="0" suppressed="false"/>
                    <ns2:Issue instanceId="%s" revision="0" suppressed="false"/>
                  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(AUDIT_NS, namedId);
    }

    private String minimalAuditFvdl() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <FVDL>
                  <UUID>uuid-1</UUID>
                  <Build>
                    <BuildID>build-1</BuildID>
                    <SourceBasePath>.</SourceBasePath>
                    <NumberFiles>1</NumberFiles>
                    <ScanTime>1</ScanTime>
                  </Build>
                </FVDL>
                """;
    }
}
