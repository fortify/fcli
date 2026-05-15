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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;

class AuditProcessorSuppressionExclusionsTest {
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
    void testUpdateAndSaveDoesNotSuppressExcludedCategory() throws Exception {
        createTestFpr(createAuditXml(false));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-1", createFalsePositiveResponse()),
                createTagMappingConfig("Privacy Violation"),
                Map.of("instance-1", "Privacy Violation"),
                new FPRInfo(fprHandle));

        assertEquals("false", readIssueElement().getAttribute("suppressed"));
    }

    @Test
    void testUpdateAndSaveSuppressesNonExcludedCategory() throws Exception {
        createTestFpr(createAuditXml(false));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        auditProcessor.updateAndSaveAuditAndRemediationsXml(
                Map.of("instance-1", createFalsePositiveResponse()),
                createTagMappingConfig("Privacy Violation"),
                Map.of("instance-1", "Cross-Site Scripting"),
                new FPRInfo(fprHandle));

        assertEquals("true", readIssueElement().getAttribute("suppressed"));
    }

    @Test
    void testUpdateAndSaveThrowsClearErrorWhenCategoryLookupMissing() throws Exception {
        createTestFpr(createAuditXml(false));
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();

        AviatorTechnicalException exception = assertThrows(
                AviatorTechnicalException.class,
                () -> auditProcessor.updateAndSaveAuditAndRemediationsXml(
                        Map.of("instance-1", createFalsePositiveResponse()),
                        createTagMappingConfig("Privacy Violation"),
                        Map.of(),
                        new FPRInfo(fprHandle)));

        assertEquals(
                "Cannot apply suppression exclusions for issue 'instance-1' because no vulnerability category was available.",
                exception.getMessage());
    }

    private void createTestFpr(String auditXml) throws Exception {
        tempFprFile = Files.createTempFile("audit-processor-suppressions", ".fpr");
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

    private Element readIssueElement() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document;
        try (var inputStream = Files.newInputStream(fprHandle.getPath("/audit.xml"))) {
            document = factory.newDocumentBuilder().parse(inputStream);
        }
        return (Element) document.getElementsByTagNameNS("xmlns://www.fortify.com/schema/audit", "Issue").item(0);
    }

    private AuditResponse createFalsePositiveResponse() {
        return AuditResponse.builder()
                .issueId("instance-1")
                .status("SUCCESS")
                .tier("GOLD")
                .auditResult(AuditResult.builder()
                        .tagValue(Constants.NOT_AN_ISSUE)
                        .comment("Reviewed by Aviator")
                        .build())
                .build();
    }

    private TagMappingConfig createTagMappingConfig(String... suppressionExcludedCategories) {
        TagMappingConfig config = new TagMappingConfig();
        config.setSuppression_exclusions(new ArrayList<>(List.of(createSuppressionExclusion(suppressionExcludedCategories))));

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

    private TagMappingConfig.SuppressionExclusion createSuppressionExclusion(String... categories) {
        TagMappingConfig.SuppressionExclusion suppressionExclusion = new TagMappingConfig.SuppressionExclusion();
        suppressionExclusion.setCategories(new ArrayList<>(List.of(categories)));
        return suppressionExclusion;
    }

    private String createAuditXml(boolean suppressed) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="xmlns://www.fortify.com/schema/audit" version="4.4">
                  <ns2:IssueList>
                    <ns2:Issue instanceId="instance-1" revision="0" suppressed="%s"/>
                  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(suppressed);
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
