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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;

class AuditProcessorSuppressionHistoryTest {
    private static final String AUDIT_NAMESPACE_URI = "xmlns://www.fortify.com/schema/audit";
    private static final String INSTANCE_ID = "ISSUE-1";

    @TempDir
    Path tempDir;

    private FprHandle fprHandle;

    @AfterEach
    void tearDown() throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
        }
    }

    @Test
    void shouldAddSuppressionHistoryWhenAviatorSuppressesAnExistingIssue() throws Exception {
        fprHandle = new FprHandle(createTestFpr(minimalAuditXml()));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Element issueElement = auditProcessor.findIssueElement(INSTANCE_ID);
        auditProcessor.updateIssueElement(issueElement, createAuditResponse("GOLD", Constants.NOT_AN_ISSUE), createTagMappingConfig(), Map.of());

        assertEquals("true", issueElement.getAttribute("suppressed"));
        assertEquals(1, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID));
        assertEquals(1, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID, Boolean.TRUE.toString()));
    }

    @Test
    void shouldNotAddSuppressionHistoryWhenMappingDoesNotSuppress() throws Exception {
        fprHandle = new FprHandle(createTestFpr(minimalAuditXml()));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Element issueElement = auditProcessor.findIssueElement(INSTANCE_ID);
        auditProcessor.updateIssueElement(issueElement, createAuditResponse("SILVER", Constants.NOT_AN_ISSUE), createTagMappingConfig(), Map.of());

        assertNotEquals("true", issueElement.getAttribute("suppressed"));
        assertEquals(0, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID));
        assertEquals(0, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID, Boolean.TRUE.toString()));
    }

    @Test
    void shouldAddUnsuppressionHistoryWhenAviatorClearsAPreviouslySuppressedIssue() throws Exception {
        fprHandle = new FprHandle(createTestFpr(minimalAuditXml(true)));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Element issueElement = auditProcessor.findIssueElement(INSTANCE_ID);
        auditProcessor.updateIssueElement(issueElement, createAuditResponse("SILVER", Constants.NOT_AN_ISSUE), createTagMappingConfig(), Map.of());

        assertEquals("false", issueElement.getAttribute("suppressed"));
        assertEquals(1, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID));
        assertEquals(1, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID, Boolean.FALSE.toString()));
    }

    @Test
    void shouldNotAddSuppressionHistoryWhenIssueIsAlreadySuppressedAndRemainsSuppressed() throws Exception {
        fprHandle = new FprHandle(createTestFpr(minimalAuditXml(true)));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        Element issueElement = auditProcessor.findIssueElement(INSTANCE_ID);
        auditProcessor.updateIssueElement(issueElement, createAuditResponse("GOLD", Constants.NOT_AN_ISSUE), createTagMappingConfig(), Map.of());

        assertEquals("true", issueElement.getAttribute("suppressed"));
        assertEquals(0, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID));
    }

    @Test
    void shouldAddSuppressionHistoryWhenAviatorSuppressesANewIssue() throws Exception {
        fprHandle = new FprHandle(createTestFpr(emptyAuditXml()));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.addNewIssueElement(INSTANCE_ID, createAuditResponse("GOLD", Constants.NOT_AN_ISSUE), createTagMappingConfig(), Map.of());

        Element issueElement = auditProcessor.findIssueElement(INSTANCE_ID);
        assertEquals("true", issueElement.getAttribute("suppressed"));
        assertEquals(1, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID));
        assertEquals(1, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID, Boolean.TRUE.toString()));
    }

    @Test
    void shouldNotAddSuppressionHistoryWhenANewIssueRemainsUnsuppressed() throws Exception {
        fprHandle = new FprHandle(createTestFpr(emptyAuditXml()));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.addNewIssueElement(INSTANCE_ID, createAuditResponse("SILVER", Constants.NOT_AN_ISSUE), createTagMappingConfig(), Map.of());

        Element issueElement = auditProcessor.findIssueElement(INSTANCE_ID);
        assertEquals("false", issueElement.getAttribute("suppressed"));
        assertEquals(0, countTagHistory(issueElement, Constants.SUPPRESSED_TAG_ID));
    }

    private Path createTestFpr(String auditXml) throws Exception {
        Path fprPath = Files.createTempFile(tempDir, "audit-processor", ".fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("audit.xml"));
            zipOutputStream.write(auditXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zipOutputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index/>".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }

    private AuditResponse createAuditResponse(String tier, String tagValue) {
        return AuditResponse.builder()
                .issueId(INSTANCE_ID)
                .tier(tier)
                .auditResult(AuditResult.builder()
                        .tagValue(tagValue)
                        .comment("Processed by Aviator")
                        .build())
                .build();
    }

    private TagMappingConfig createTagMappingConfig() {
        TagMappingConfig.Result goldFp = new TagMappingConfig.Result();
        goldFp.setValue(Constants.NOT_AN_ISSUE);
        goldFp.setSuppress(true);

        TagMappingConfig.Result silverFp = new TagMappingConfig.Result();
        silverFp.setValue(Constants.PROPOSED_NOT_AN_ISSUE);
        silverFp.setSuppress(false);

        TagMappingConfig.Tier tier1 = new TagMappingConfig.Tier();
        tier1.setFp(goldFp);

        TagMappingConfig.Tier tier2 = new TagMappingConfig.Tier();
        tier2.setFp(silverFp);

        TagMappingConfig.Mapping mapping = new TagMappingConfig.Mapping();
        mapping.setTier_1(tier1);
        mapping.setTier_2(tier2);

        TagMappingConfig config = new TagMappingConfig();
        config.setTag_id(Constants.ANALYSIS_TAG_ID);
        config.setMapping(mapping);
        return config;
    }

    private int countTagHistory(Element issueElement, String tagId) {
        return countTagHistory(issueElement, tagId, null);
    }

    private int countTagHistory(Element issueElement, String tagId, String value) {
        NodeList tagHistories = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "TagHistory");
        int count = 0;
        for (int index = 0; index < tagHistories.getLength(); index++) {
            Element tagHistory = (Element) tagHistories.item(index);
            NodeList tags = tagHistory.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Tag");
            if (tags.getLength() == 0) {
                continue;
            }

            Element tag = (Element) tags.item(0);
            if (!tagId.equals(tag.getAttribute("id"))) {
                continue;
            }

            NodeList values = tag.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Value");
            if (value == null) {
                count++;
            } else if (values.getLength() > 0 && value.equals(values.item(0).getTextContent())) {
                count++;
            }
        }
        return count;
    }

    private String minimalAuditXml() {
        return minimalAuditXml(false);
    }

    private String emptyAuditXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="xmlns://www.fortify.com/schema/audit" version="4.4">
                  <ns2:IssueList/>
                </ns2:Audit>
                """;
    }

    private String minimalAuditXml(boolean suppressed) {
        String suppressedAttribute = suppressed ? " suppressed=\"true\"" : "";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="xmlns://www.fortify.com/schema/audit" version="4.4">
                  <ns2:IssueList>
                    <ns2:Issue instanceId="ISSUE-1" revision="0"%s/>
                  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(suppressedAttribute);
    }
}