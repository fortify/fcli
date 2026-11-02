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
package com.fortify.cli.common.fpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.common.fpr.processor.AuditXmlReader;
import com.fortify.cli.common.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.utils.SourceDecoders;

class SastFprParserTest {
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
    void parseReturnsSameVulnerabilitiesAsManualProcessorWiring() throws Exception {
        createTestFpr();

        SastFprData parsed = SastFprParser.parse(fprHandle, SourceDecoders.defaults());

        List<Vulnerability> expected = new FPRProcessor(fprHandle, new AuditXmlReader().read(fprHandle).auditIssues())
            .process(new StreamingFVDLProcessor(fprHandle));

        assertEquals(1, parsed.vulnerabilities().size());
        assertEquals(expected.size(), parsed.vulnerabilities().size());
        Vulnerability actualVuln = parsed.vulnerabilities().get(0);
        Vulnerability expectedVuln = expected.get(0);
        assertEquals(expectedVuln.getInstanceID(), actualVuln.getInstanceID());
        assertEquals(expectedVuln.getClassID(), actualVuln.getClassID());
        assertEquals(expectedVuln.getCategory(), actualVuln.getCategory());
        assertEquals("instance-1", actualVuln.getInstanceID());
    }

    @Test
    void parseExposesDataUsedForFollowUpOperations() throws Exception {
        createTestFpr();

        SastFprData parsed = SastFprParser.parse(fprHandle, SourceDecoders.defaults());

        assertNotNull(parsed.auditDocument());
        assertNotNull(parsed.auditIssueMap());
        assertNotNull(parsed.streamingFVDLProcessor());
        assertNotNull(parsed.fprInfo());
    }

    @Test
    void parseRejectsMissingArguments() throws Exception {
        createTestFpr();

        assertThrows(NullPointerException.class, () -> SastFprParser.parse(null, SourceDecoders.defaults()));
        assertThrows(NullPointerException.class, () -> SastFprParser.parse(fprHandle, null));
    }

    private void createTestFpr() throws Exception {
        tempFprFile = Files.createTempFile("sast-fpr-parser", ".fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {
            zip.putNextEntry(new ZipEntry("audit.fvdl"));
            zip.write(minimalAuditFvdl().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zip.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        fprHandle = new FprHandle(tempFprFile);
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
}
