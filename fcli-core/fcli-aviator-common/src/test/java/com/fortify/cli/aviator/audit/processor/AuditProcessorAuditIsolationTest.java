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
package com.fortify.cli.aviator.audit.processor;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;
import com.fortify.cli.aviator.audit.model.Autoremediation;
import com.fortify.cli.aviator.audit.model.Change;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.fpr.model.FPRInfo;
import com.fortify.cli.common.fpr.util.FprHandle;

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
    void statusTracksRemediationAvailabilityAndReauditTransitions() throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-A", responseWithChanges("instance-A", List.of(
                        Change.builder().file("src/Example.java").fromLine("1").toLine("1").replaceWith("fixed").build()))),
                createTagMappingConfig(),
                Map.of("instance-A", "SQL Injection"),
                new FPRInfo(fprHandle));

        Element audited = readIssueElement("instance-A");
        assertEquals("PROCESSED_BY_AVIATOR_WITH_REMEDIATION", readAviatorStatus(audited));
        assertEquals(List.of("PROCESSED_BY_AVIATOR_WITH_REMEDIATION"), readAviatorStatusHistory(audited));

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-A", responseWithChanges("instance-A", List.of())),
                createTagMappingConfig(),
                Map.of("instance-A", "SQL Injection"),
                new FPRInfo(fprHandle));

        audited = readIssueElement("instance-A");
        assertEquals(Constants.PROCESSED_BY_AVIATOR, readAviatorStatus(audited));
        assertEquals(List.of(
                "PROCESSED_BY_AVIATOR_WITH_REMEDIATION",
                Constants.PROCESSED_BY_AVIATOR), readAviatorStatusHistory(audited));

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", responseWithChanges("instance-A", List.of(validChange()))),
            createTagMappingConfig(), Map.of(), new FPRInfo(fprHandle));
        audited = readIssueElement("instance-A");
        assertEquals(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION, readAviatorStatus(audited));
        assertEquals(List.of(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION, Constants.PROCESSED_BY_AVIATOR,
            Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION), readAviatorStatusHistory(audited));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingSourceDoesNotClaimRemediationOrCountFallback(boolean legacyStatus) throws Exception {
        createTestFpr(multiIssueAuditXml("instance-A"));
        AuditProcessor processor = new AuditProcessor(fprHandle);
        processor.processAuditXML();

        processor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", responseWithChanges("instance-A", List.of(validChange()))),
            createTagMappingConfig(), Map.of(), new FPRInfo(fprHandle), null, legacyStatus);

        assertStatusAndHistory("instance-A", Constants.PROCESSED_BY_AVIATOR);
        assertEquals(0, processor.getLastRemediationGenerationMetric().legacyStatusFallbacks());
        assertTrue(Files.notExists(fprHandle.getPath("/remediations.xml")));
        assertTrue(processor.getLastRemediationGenerationMetric().skippedByReason()
            .containsKey("Source file read/decode failed"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mixedIssuesAndChangesTrackOnlyEmittedRemediations(boolean legacyStatus) throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A", "untouched"));
        AuditProcessor processor = new AuditProcessor(fprHandle);
        processor.processAuditXML();
        Change invalid = Change.builder().file("src/Example.java").fromLine("invalid").toLine("1").build();
        Change negative = Change.builder().file("src/Example.java").fromLine("-1").toLine("1").build();
        Change missing = Change.builder().file("src/Missing.java").fromLine("1").toLine("1").build();

        processor.updateAndSaveAuditAndRemediationsXml(Map.of(
            "instance-A", responseWithChanges("instance-A", List.of(invalid, negative, missing, validChange())),
            "new-valid", responseWithChanges("new-valid", List.of(validChange())),
            "new-invalid", responseWithChanges("new-invalid", List.of(invalid, negative)),
            "new-missing", responseWithChanges("new-missing", List.of(missing))),
            createTagMappingConfig(), Map.of(), new FPRInfo(fprHandle), null, legacyStatus);

        String emittedStatus = legacyStatus ? Constants.PROCESSED_BY_AVIATOR
            : Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION;
        assertStatusAndHistory("instance-A", emittedStatus);
        assertStatusAndHistory("new-valid", emittedStatus);
        assertStatusAndHistory("new-invalid", Constants.PROCESSED_BY_AVIATOR);
        assertStatusAndHistory("new-missing", Constants.PROCESSED_BY_AVIATOR);
        assertEquals(Set.of("instance-A", "new-valid", "new-invalid", "new-missing"), readIssueInstanceIds());
        assertEquals(legacyStatus ? 2 : 0, processor.getLastRemediationGenerationMetric().legacyStatusFallbacks());
        Document remediations = readRemediationsDocument();
        NodeList emitted = remediations.getElementsByTagNameNS("*", "Remediation");
        Set<String> emittedIds = new HashSet<>();
        for (int i = 0; i < emitted.getLength(); i++) {
            emittedIds.add(((Element) emitted.item(i)).getAttribute("instanceId"));
        }
        assertEquals(Set.of("instance-A", "new-valid"), emittedIds);
        assertEquals(2, remediations.getElementsByTagNameNS("*", "Change").getLength());
        assertTrue(processor.getLastRemediationGenerationMetric().skippedByReason()
            .containsKey("Structurally invalid remediation"));
    }

    @Test
    void auditOnlyEntryPointsDoNotClaimUnwrittenRemediation() throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A"));
        AuditProcessor processor = new AuditProcessor(fprHandle);
        processor.processAuditXML();
        processor.updateIssueElement(processor.findIssueElement("instance-A"),
            responseWithChanges("instance-A", List.of(validChange())), createTagMappingConfig(), Map.of());
        processor.addNewIssueElement("new", responseWithChanges("new", List.of(validChange())),
            createTagMappingConfig(), Map.of());

        for (String id : List.of("instance-A", "new")) {
            Element issue = processor.findIssueElement(id);
            assertEquals(Constants.PROCESSED_BY_AVIATOR, readAviatorStatus(issue));
            assertEquals(List.of(Constants.PROCESSED_BY_AVIATOR), readAviatorStatusHistory(issue));
        }
        assertTrue(Files.notExists(fprHandle.getPath("/remediations.xml")));
    }

    @Test
    void statusHistoryWithoutCurrentTagIsNotOverwritten() throws Exception {
        createTestFpr("""
            <Audit xmlns="xmlns://www.fortify.com/schema/audit" version="4.4">
              <IssueList><Issue instanceId="instance-A" revision="1">
                <ClientAuditTrail><TagHistory>
                  <Tag id="%s"><Value>%s</Value></Tag>
                  <EditTime>2026-01-01T00:00:00.000Z</EditTime><Username>previous-user</Username>
                </TagHistory></ClientAuditTrail>
              </Issue></IssueList>
            </Audit>
            """.formatted(Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION));
        AuditProcessor processor = new AuditProcessor(fprHandle);
        processor.processAuditXML();
        processor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", successResponse("instance-A")), createTagMappingConfig(), Map.of(),
            new FPRInfo(fprHandle));

        Element issue = readIssueElement("instance-A");
        assertEquals(Constants.PROCESSED_BY_AVIATOR, readAviatorStatus(issue));
        assertEquals(List.of(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION, Constants.PROCESSED_BY_AVIATOR),
            readAviatorStatusHistory(issue));
        Element history = (Element) issue.getElementsByTagNameNS(AUDIT_NS, "TagHistory").item(0);
        assertEquals("2026-01-01T00:00:00.000Z", history.getElementsByTagNameNS(AUDIT_NS, "EditTime").item(0).getTextContent());
        assertEquals("previous-user", history.getElementsByTagNameNS(AUDIT_NS, "Username").item(0).getTextContent());
    }

    @Test
    void rejectedReauditPreservesHistoricalRemediationStatus() throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A"));
        AuditProcessor processor = new AuditProcessor(fprHandle);
        processor.processAuditXML();
        processor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", responseWithChanges("instance-A", List.of(validChange()))),
            createTagMappingConfig(), Map.of(), new FPRInfo(fprHandle));
        processor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", responseWithChanges("instance-A", List.of(
                Change.builder().file("src/Example.java").fromLine("invalid").toLine("1").build()))),
            createTagMappingConfig(), Map.of(), new FPRInfo(fprHandle));

        Element issue = readIssueElement("instance-A");
        assertEquals(Constants.PROCESSED_BY_AVIATOR, readAviatorStatus(issue));
        assertEquals(List.of(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION, Constants.PROCESSED_BY_AVIATOR),
            readAviatorStatusHistory(issue));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void silentlySkippedChangesDoNotProduceOutputOrFallback(boolean legacyStatus) throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A"));
        AuditProcessor processor = new AuditProcessor(fprHandle);
        processor.processAuditXML();
        AuditResponse skipped = responseWithChanges("instance-A", List.of(validChange()));
        skipped.setStatus("SKIPPED");
        skipped.getAuditResult().setComment(null);
        processor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", skipped), createTagMappingConfig(), Map.of(),
            new FPRInfo(fprHandle), null, legacyStatus);

        assertEquals(Set.of(), readIssueInstanceIds());
        assertTrue(Files.notExists(fprHandle.getPath("/remediations.xml")));
        assertEquals(0, processor.getLastRemediationGenerationMetric().legacyStatusFallbacks());
    }

    private Change validChange() {
        return Change.builder().file("src/Example.java").fromLine("1").toLine("1").replaceWith("fixed").build();
    }

    private void assertStatusAndHistory(String instanceId, String expected) throws Exception {
        Element issue = readIssueElement(instanceId);
        assertEquals(expected, readAviatorStatus(issue));
        assertEquals(List.of(expected), readAviatorStatusHistory(issue));
    }

    private Document readRemediationsDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (var input = Files.newInputStream(fprHandle.getPath("/remediations.xml"))) {
            return factory.newDocumentBuilder().parse(input);
        }
    }

    @Test
    void legacyRemediationStatusFallbackPreservesRemediationsAndHistoricalStatusOnReaudit() throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-A"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();
        List<Change> changes = List.of(
            Change.builder().file("src/Example.java").fromLine("1").toLine("1").replaceWith("fixed").build());

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", responseWithChanges("instance-A", changes)),
            createTagMappingConfig(),
            Map.of("instance-A", "SQL Injection"),
            new FPRInfo(fprHandle));
        assertEquals(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION,
            readAviatorStatus(readIssueElement("instance-A")));
        assertEquals(0, auditProcessor.getLastRemediationGenerationMetric().legacyStatusFallbacks());

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", responseWithChanges("instance-A", changes)),
            createTagMappingConfig(),
            Map.of("instance-A", "SQL Injection"),
            new FPRInfo(fprHandle), null, true);

        assertEquals(1, auditProcessor.getLastRemediationGenerationMetric().legacyStatusFallbacks());
        Element audited = readIssueElement("instance-A");
        assertEquals(Constants.PROCESSED_BY_AVIATOR, readAviatorStatus(audited));
        assertEquals(List.of(
            Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION,
            Constants.PROCESSED_BY_AVIATOR), readAviatorStatusHistory(audited));
        assertTrue(Files.exists(fprHandle.getPath("/remediations.xml")));
        String remediationsXml = Files.readString(fprHandle.getPath("/remediations.xml"));
        assertTrue(remediationsXml.contains("instance-A"));
        assertTrue(remediationsXml.contains("fixed"));
    }

    @Test
    void legacyStatusFallbackWithoutRemediationIsNotCounted() throws Exception {
        createTestFpr(multiIssueAuditXml("instance-A"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
            Map.of("instance-A", responseWithChanges("instance-A", List.of())),
            createTagMappingConfig(),
            Map.of("instance-A", "SQL Injection"),
            new FPRInfo(fprHandle), null, true);

        assertEquals(Constants.PROCESSED_BY_AVIATOR, readAviatorStatus(readIssueElement("instance-A")));
        assertEquals(0, auditProcessor.getLastRemediationGenerationMetric().legacyStatusFallbacks());
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
        Element newIssue = readIssueElement("instance-new");
        assertEquals(Constants.PROCESSED_BY_AVIATOR, readAviatorStatus(newIssue));
        assertEquals(List.of(Constants.PROCESSED_BY_AVIATOR), readAviatorStatusHistory(newIssue));
    }

    @Test
    void newIssueReceivesRemediationStatusAndHistory() throws Exception {
        createTestFprWithSource(multiIssueAuditXml("instance-old"));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-new", responseWithChanges("instance-new", List.of(
                        Change.builder().file("src/Example.java").fromLine("1").toLine("1").replaceWith("fixed").build()))),
                createTagMappingConfig(),
                Map.of("instance-new", "SQL Injection"),
                new FPRInfo(fprHandle));

        assertEquals(Set.of("instance-new"), readIssueInstanceIds());
        Element newIssue = readIssueElement("instance-new");
        assertEquals(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION, readAviatorStatus(newIssue));
        assertEquals(List.of(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION),
                readAviatorStatusHistory(newIssue));
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
        createTestFpr(auditXml, false);
    }

    private void createTestFprWithSource(String auditXml) throws Exception {
        createTestFpr(auditXml, true);
    }

    private void createTestFpr(String auditXml, boolean includeSource) throws Exception {
        tempFprFile = Files.createTempFile("audit-processor-isolation", ".fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {
            zipOutputStream.putNextEntry(new ZipEntry("audit.fvdl"));
            zipOutputStream.write(minimalAuditFvdl().getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("audit.xml"));
            zipOutputStream.write(auditXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("src-archive/index.xml"));
            String sourceIndex = includeSource
                ? "<?xml version=\"1.0\" encoding=\"UTF-8\"?><properties>"
                    + "<entry key=\"src/Example.java\">src-archive/1</entry></properties>"
                : "<?xml version=\"1.0\" encoding=\"UTF-8\"?><index/>";
            zipOutputStream.write(sourceIndex.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            if (includeSource) {
                zipOutputStream.putNextEntry(new ZipEntry("src-archive/1"));
                zipOutputStream.write("class Example { int value = 1; }".getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
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

    private String readAviatorStatus(Element issueElement) {
        NodeList tags = issueElement.getElementsByTagNameNS(AUDIT_NS, "Tag");
        for (int i = 0; i < tags.getLength(); i++) {
            Element tag = (Element) tags.item(i);
            if (Constants.AVIATOR_STATUS_TAG_ID.equalsIgnoreCase(tag.getAttribute("id"))) {
                return tag.getElementsByTagNameNS(AUDIT_NS, "Value").item(0).getTextContent();
            }
        }
        return null;
    }

    private List<String> readAviatorStatusHistory(Element issueElement) {
        List<String> values = new ArrayList<>();
        NodeList histories = issueElement.getElementsByTagNameNS(AUDIT_NS, "TagHistory");
        for (int i = 0; i < histories.getLength(); i++) {
            Element history = (Element) histories.item(i);
            NodeList tags = history.getElementsByTagNameNS(AUDIT_NS, "Tag");
            if (tags.getLength() == 0) {
                continue;
            }
            Element tag = (Element) tags.item(0);
            if (Constants.AVIATOR_STATUS_TAG_ID.equalsIgnoreCase(tag.getAttribute("id"))) {
                values.add(tag.getElementsByTagNameNS(AUDIT_NS, "Value").item(0).getTextContent());
            }
        }
        return values;
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

    private AuditResponse responseWithChanges(String instanceId, List<Change> changes) {
        return AuditResponse.builder()
                        .issueId(instanceId)
                        .status("SUCCESS")
                        .tier("GOLD")
                        .auditResult(AuditResult.builder()
                                .tagValue(Constants.NOT_AN_ISSUE)
                                .comment("Reviewed by Aviator")
                                .autoremediation(Autoremediation.builder().changes(changes).build())
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
