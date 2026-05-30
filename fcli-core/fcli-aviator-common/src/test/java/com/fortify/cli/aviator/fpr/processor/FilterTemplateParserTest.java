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

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.util.FprHandle;

@DisplayName("FilterTemplateParser")
class FilterTemplateParserTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("throws AviatorTechnicalException on malformed filtertemplate.xml")
    void throwsTechnicalExceptionOnMalformedFilterTemplateXml() throws Exception {
        Path fprPath = createFpr("<FilterTemplate>");

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            FilterTemplateParser parser = new FilterTemplateParser(fprHandle, new AuditProcessor(fprHandle));
            AviatorTechnicalException exception = assertThrows(AviatorTechnicalException.class, parser::parseFilterTemplate);

            assertTrue(exception.getMessage().contains("filtertemplate.xml"));
        }
    }

    private Path createFpr(String filterTemplateXml) throws IOException {
        Path fprPath = tempDir.resolve("test.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zipOutputStream, "filtertemplate.xml", filterTemplateXml);
            writeEntry(zipOutputStream, "src-archive/index.xml", """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <index>
                    <entry key=\"Test.java\">src-archive/Test.java</entry>
                </index>
                """);
            writeEntry(zipOutputStream, "src-archive/Test.java", "public class Test {}\n");
        }
        return fprPath;
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }
}