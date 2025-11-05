/*
 * Copyright 2021-2025 Open Text.
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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliBugException;

class RemediationProcessorTest {

    @TempDir
    Path tempDir;

    private Path createTestFpr(String remediationsXmlContent) throws IOException {
        Path fprPath = tempDir.resolve("test.fpr");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            ZipEntry entry = new ZipEntry("remediations.xml");
            zos.putNextEntry(entry);
            zos.write(remediationsXmlContent.getBytes());
            zos.closeEntry();
        }
        return fprPath;
    }

    @Test
    void testProcessRemediationXML_NullSourceDir() throws IOException {
        String remediationXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Remediations xmlns="xmlns://www.fortify.com/schema/remediations">
            </Remediations>
            """;
        Path fprPath = createTestFpr(remediationXml);

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            assertThrows(FcliBugException.class, () -> new RemediationProcessor(fprHandle, null));
        }
    }

    @Test
    void testProcessRemediationXML_BlankSourceDir() throws IOException {
        String remediationXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Remediations xmlns="xmlns://www.fortify.com/schema/remediations">
            </Remediations>
            """;
        Path fprPath = createTestFpr(remediationXml);

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            assertThrows(FcliBugException.class, () -> new RemediationProcessor(fprHandle, ""));
            assertThrows(FcliBugException.class, () -> new RemediationProcessor(fprHandle, "   "));
        }
    }

}
