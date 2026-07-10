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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AviatorSSCCorrelateFprParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parseSastFprAllowsMissingAuditXml() throws Exception {
        Path fprPath = createMinimalSastFpr(false);

        AviatorSSCCorrelateFprParser.ParseResult result = AviatorSSCCorrelateFprParser.parseSastFpr(fprPath);

        assertEquals(1, result.vulnerabilities.size());
        assertNotNull(result.auditIssueMap);
        assertTrue(result.auditIssueMap.isEmpty());
    }

    @Test
    void parseDastFprAllowsMissingAuditXml() throws Exception {
        Path fprPath = createMinimalDastFpr(true, false);

        AviatorSSCCorrelateFprParser.ParseResult result = AviatorSSCCorrelateFprParser.parseDastFpr(fprPath);

        assertEquals(1, result.dastIssues.size());
        assertNotNull(result.auditIssueMap);
        assertTrue(result.auditIssueMap.isEmpty());
        assertEquals("Injection", result.dastIssues.get(0).getCategory());
    }

    @Test
    void parseDastFprPreservesNullCategoryWhen7pkCategoryIsMissing() throws Exception {
        Path fprPath = createMinimalDastFpr(false, false);

        AviatorSSCCorrelateFprParser.ParseResult result = AviatorSSCCorrelateFprParser.parseDastFpr(fprPath);

        assertEquals(1, result.dastIssues.size());
        assertNull(result.dastIssues.get(0).getCategory());
        assertEquals("SQL Injection", result.dastIssues.get(0).getName());
    }

    private Path createMinimalSastFpr(boolean includeAuditXml) throws Exception {
        Path fprPath = tempDir.resolve(includeAuditXml ? "sast-with-audit.fpr" : "sast-no-audit.fpr");
        if (Files.exists(fprPath)) {
            Files.delete(fprPath);
        }

        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, Map.of("create", "true"))) {
            Files.writeString(zipFs.getPath("/audit.fvdl"), minimalAuditFvdl(), StandardCharsets.UTF_8);
            Files.createDirectories(zipFs.getPath("/src-archive"));
            Files.writeString(zipFs.getPath("/src-archive/index.xml"), indexXml(), StandardCharsets.UTF_8);
            Files.writeString(zipFs.getPath("/src-archive/Test.java"), "class Test {}\n", StandardCharsets.UTF_8);
            if (includeAuditXml) {
                Files.writeString(zipFs.getPath("/audit.xml"), minimalAuditXml(), StandardCharsets.UTF_8);
            }
        }

        return fprPath;
    }

    private Path createMinimalDastFpr(boolean includeCategory, boolean includeAuditXml) throws Exception {
        Path fprPath = tempDir.resolve(includeAuditXml ? "dast-with-audit.fpr" : "dast-no-audit.fpr");
        if (Files.exists(fprPath)) {
            Files.delete(fprPath);
        }

        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, Map.of("create", "true"))) {
            Files.writeString(zipFs.getPath("/webinspect.xml"), webInspectXml(includeCategory), StandardCharsets.UTF_8);
            if (includeAuditXml) {
                Files.writeString(zipFs.getPath("/audit.xml"), minimalAuditXml(), StandardCharsets.UTF_8);
            }
        }

        return fprPath;
    }

    private String minimalAuditFvdl() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <FVDL>
              <Vulnerabilities>
                <Vulnerability>
                  <ClassInfo>
                    <ClassID>RULE-1</ClassID>
                    <Kingdom>Dataflow</Kingdom>
                    <Type>Cross-Site Scripting</Type>
                    <Subtype>Reflected</Subtype>
                    <AnalyzerName>Dataflow</AnalyzerName>
                    <DefaultSeverity>3.0</DefaultSeverity>
                  </ClassInfo>
                  <InstanceInfo>
                    <InstanceID>instance-1</InstanceID>
                    <InstanceSeverity>3.0</InstanceSeverity>
                    <Confidence>4.0</Confidence>
                  </InstanceInfo>
                </Vulnerability>
              </Vulnerabilities>
            </FVDL>
            """;
    }

    private String minimalAuditXml() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Audit xmlns="xmlns://www.fortify.com/schema/audit">
              <ProjectInfo>
                <Name>TestProject</Name>
              </ProjectInfo>
              <IssueList/>
            </Audit>
            """;
    }

    private String indexXml() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <index>
                <entry key="Test.java">src-archive/Test.java</entry>
            </index>
            """;
    }

    private String webInspectXml(boolean includeCategory) {
        String categoryClassification = includeCategory
                ? "<Classification kind=\"7PK Category\">Injection</Classification>"
                : "";
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ScanResults>
                <Session requestId="REQ-1">
                    <URL>https://example.test/login</URL>
                    <Issues>
                        <Issue id="DAST-1">
                            <CheckTypeID>1001</CheckTypeID>
                            <EngineType>WebInspect</EngineType>
                            <VulnerabilityID>WI-1001</VulnerabilityID>
                            <Severity>4</Severity>
                            <Name>SQL Injection</Name>
                            <Classifications>
                                %s
                                <Classification kind="CWE" identifier="89">Improper Neutralization</Classification>
                            </Classifications>
                            <ReproSteps>
                                <ReproStep>
                                    <Url>https://example.test/login</Url>
                                </ReproStep>
                            </ReproSteps>
                            <ReportSection>
                                <Name>Summary</Name>
                                <SectionText><![CDATA[Unsanitized user input reaches the SQL query.]]></SectionText>
                            </ReportSection>
                        </Issue>
                    </Issues>
                </Session>
            </ScanResults>
            """.formatted(categoryClassification);
    }
}