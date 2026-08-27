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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.util.FprHandle;

class RemediationProcessorTest {
    private static final String REMEDIATIONS_NAMESPACE = "xmlns://www.fortify.com/schema/remediations";

    @TempDir
    Path tempDir;

    @Test
    void skipsAmbiguousContextWithoutChangingSource() throws Exception {
        String originalSource = "before\nTARGET\nafter\nbefore\nTARGET\nafter\n";
        Path sourceFile = writeSourceFile(originalSource);
        Path fprPath = createRemediationFpr(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED");

        RemediationProcessor.RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.totalRemediations());
        assertEquals(0, metric.appliedRemediations());
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of(
                "Source context matched multiple locations in file 'Example.java'; candidate lines: 1, 4", 1),
                metric.skippedByReason());
        assertEquals(originalSource, Files.readString(sourceFile));
    }

    @Test
    void appliesRemediationWhenContextMatchesOnce() throws Exception {
        Path sourceFile = writeSourceFile("before\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED");

        RemediationProcessor.RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    @Test
    void appliesOriginalCodeAfterLeadingContextLines() throws Exception {
        Path sourceFile = writeSourceFile("TARGET\nkeep\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(3, 3, 2, 1, "TARGET\nkeep\nTARGET\nafter", "TARGET", "REPLACED");

        RemediationProcessor.RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
          metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals("TARGET\nkeep\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

      @Test
      void appliesRemediationWhenContextStartsWithBlankLine() throws Exception {
        Path sourceFile = writeSourceFile("header\n\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(3, 3, 1, 1, "\nTARGET\nafter", "TARGET", "REPLACED");

        RemediationProcessor.RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
          metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals("header\n\nREPLACED\nafter\n", Files.readString(sourceFile));
      }

    private Path writeSourceFile(String content) throws Exception {
        Path sourceFile = tempDir.resolve("Example.java");
        Files.writeString(sourceFile, content, StandardCharsets.UTF_8);
        return sourceFile;
    }

    private Path createRemediationFpr(String context, String originalCode, String newCode) throws Exception {
        return createRemediationFpr(2, 2, 1, 1, context, originalCode, newCode);
    }

    private Path createRemediationFpr(int lineFrom, int lineTo, int contextBefore, int contextAfter,
            String context, String originalCode, String newCode) throws Exception {
        Path fprPath = tempDir.resolve("remediation.fpr");
        String remediationXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <r:Remediations xmlns:r="%s">
                  <r:ProjectInfo>
                    <r:Name>test</r:Name>
                    <r:WriteDate>2026-08-26T00:00:00Z</r:WriteDate>
                  </r:ProjectInfo>
                  <r:RemediationList>
                  <r:Remediation instanceId="issue-1">
                    <r:AuditComment>test</r:AuditComment>
                    <r:FileChanges>
                      <r:Filename>Example.java</r:Filename>
                      <r:Hash type="SHA-256">not-the-source-hash</r:Hash>
                      <r:Change>
                        <r:LineFrom>%d</r:LineFrom>
                        <r:LineTo>%d</r:LineTo>
                        <r:Context before="%d" after="%d">%s</r:Context>
                        <r:OriginalCode>%s</r:OriginalCode>
                        <r:NewCode>%s</r:NewCode>
                      </r:Change>
                    </r:FileChanges>
                  </r:Remediation>
                  </r:RemediationList>
                </r:Remediations>
                """.formatted(REMEDIATIONS_NAMESPACE, lineFrom, lineTo, contextBefore, contextAfter,
                context, originalCode, newCode);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("remediations.xml"));
            zipOutputStream.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }
}
