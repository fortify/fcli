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
package com.fortify.cli.aviator.dast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.util.FprHandle;

class StreamingWebInspectParserTest {
    @TempDir
    Path tempDir;

    @Test
    void parseSessionsPreservesCompleteAuditContext() throws Exception {
        Path fpr = createFpr();

        try (FprHandle handle = new FprHandle(fpr)) {
            var sessions = new StreamingWebInspectParser(handle).parseSessions();

            assertEquals(1, sessions.size());
            DastSession session = sessions.get(0);
            assertEquals("POST /login HTTP/1.1", session.getRawRequest());
            assertEquals("HTTP/1.1 200 OK", session.getRawResponse());
            assertEquals("parameter=username", session.getAttackParamDescriptor());

            DastIssue issue = session.getIssues().get(0);
            assertEquals("Injection", issue.getCategory());
            assertEquals("Improper Neutralization", issue.getCweDescription());
            assertEquals("Summary text", issue.getSummary());
            assertEquals("Fix text", issue.getFix());
            assertEquals(2, issue.getReproSteps().size());
            assertEquals("Macro", issue.getReproSteps().get(0).getSource());
            assertEquals("Attack", issue.getReproSteps().get(1).getSource());
            assertEquals("username=test%27", issue.getReproSteps().get(1).getPostParams());
            assertEquals(issue.getReproSteps().stream().map(DastReproStep::getUrl).toList(), issue.getReproStepUrls());
        }
    }

    private Path createFpr() throws Exception {
        Path fpr = tempDir.resolve("dast.fpr");
        try (FileSystem zip = FileSystems.newFileSystem(fpr, Map.of("create", "true"))) {
            Files.writeString(zip.getPath("/webinspect.xml"), webInspectXml(), StandardCharsets.UTF_8);
        }
        return fpr;
    }

    private String webInspectXml() {
        String request = Base64.getEncoder().encodeToString("POST /login HTTP/1.1".getBytes(StandardCharsets.UTF_8));
        String response = Base64.getEncoder().encodeToString("HTTP/1.1 200 OK".getBytes(StandardCharsets.UTF_8));
        return """
            <WebInspectScan>
              <Session requestId="REQ-1">
                <URL>https://example.test/login</URL>
                <Scheme>https</Scheme><Host>example.test</Host><Port>443</Port>
                <AttackParamDescriptor>parameter=username</AttackParamDescriptor>
                <RawRequest>%s</RawRequest><RawResponse>%s</RawResponse>
                <Issues><Issue id="DAST-1">
                  <CheckTypeID>1001</CheckTypeID><EngineType>WebInspect</EngineType>
                  <VulnerabilityID>WI-1001</VulnerabilityID><Severity>4</Severity><Name>SQL Injection</Name>
                  <Classifications>
                    <Classification kind="7PK Category">Injection</Classification>
                    <Classification kind="CWE" identifier="89">Improper Neutralization</Classification>
                  </Classifications>
                  <ReproSteps>
                    <ReproStep><Source>Macro</Source><Url>https://example.test/login</Url></ReproStep>
                    <ReproStep><Source>Attack</Source><Url>https://example.test/login?user=test%%27</Url>
                      <PostParams>username=test%%27</PostParams></ReproStep>
                  </ReproSteps>
                  <ReportSection><Name>Summary</Name><SectionText><![CDATA[<p>Summary text</p>]]></SectionText></ReportSection>
                  <ReportSection><Name>Fix</Name><SectionText><![CDATA[<p>Fix text</p>]]></SectionText></ReportSection>
                </Issue></Issues>
              </Session>
            </WebInspectScan>
            """.formatted(request, response);
    }
}