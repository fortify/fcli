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
package com.fortify.cli.aviator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;

@DisplayName("FprHandle")
class FprHandleTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("parses simple index XML without DOCTYPE")
    void parsesSimpleIndexXmlWithoutDoctype() throws Exception {
        Path fprPath = createFpr("""
            <?xml version="1.0" encoding="UTF-8"?>
            <index>
                <entry key="Test.java">src-archive/Test.java</entry>
            </index>
            """);

        try (FprHandle handle = new FprHandle(fprPath)) {
            assertEquals("src-archive/Test.java", handle.getSourceFileMap().get("Test.java"));
        }
    }

    @Test
    @DisplayName("defers malformed source index parsing until source map is requested")
    void defersMalformedSourceIndexParsingUntilSourceMapIsRequested() throws Exception {
        Path fprPath = createFpr("""
            <?xml version="1.0" encoding="UTF-8"?>
            <index>
                <entry key="Test.java">src-archive/Test.java</index>
            """);

        try (FprHandle handle = new FprHandle(fprPath)) {
            AviatorTechnicalException exception = assertThrows(AviatorTechnicalException.class, handle::getSourceFileMap);

            assertTrue(exception.getCause() instanceof SAXException);
        }
    }

    @Test
    @DisplayName("validate parses source map after source presence checks")
    void validateParsesSourceMapAfterSourcePresenceChecks() throws Exception {
        Path fprPath = createFpr("""
            <?xml version="1.0" encoding="UTF-8"?>
            <index>
                <entry key="Test.java">src-archive/Test.java</index>
            """);

        try (FprHandle handle = new FprHandle(fprPath)) {
            AviatorTechnicalException exception = assertThrows(AviatorTechnicalException.class, handle::validate);

            assertTrue(exception.getCause() instanceof SAXException);
        }
    }

    @Test
    @DisplayName("opens remediation-only FPR without requiring source archive index")
    void opensRemediationOnlyFprWithoutRequiringSourceArchiveIndex() throws Exception {
        Path fprPath = createFprWithoutSourceIndex();

        try (FprHandle handle = new FprHandle(fprPath)) {
            assertTrue(handle.hasRemediations());
        }
    }

    private Path createFpr(String indexXml) throws IOException {
        Path fprPath = tempDir.resolve("test.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zipOutputStream, "audit.fvdl", "<FVDL />");
            writeEntry(zipOutputStream, "src-archive/index.xml", indexXml);
            writeEntry(zipOutputStream, "src-archive/Test.java", "public class Test {}\n");
        }
        return fprPath;
    }

    private Path createFprWithoutSourceIndex() throws IOException {
        Path fprPath = tempDir.resolve("remediation-only.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zipOutputStream, "remediations.xml", "<Remediations />");
        }
        return fprPath;
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }
}