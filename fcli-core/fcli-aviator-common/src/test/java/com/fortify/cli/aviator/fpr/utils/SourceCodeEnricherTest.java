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
package com.fortify.cli.aviator.fpr.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.util.FprHandle;

class SourceCodeEnricherTest {

    @TempDir
    Path tempDir;

    @Test
    void reportsEveryUniqueDecodeFailureAndDoesNotHideSuccessfulFiles() throws Exception {
        Map<String, byte[]> sourceFiles = new LinkedHashMap<>();
        sourceFiles.put("good.java", "class Good {}".getBytes(StandardCharsets.UTF_8));
        sourceFiles.put("bad-one.java", new byte[] {(byte) 0xFF});
        sourceFiles.put("bad-two.java", new byte[] {(byte) 0xFE});
        Path fprPath = createFpr(sourceFiles);

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            SourceCodeEnricher.EnrichmentResult result = new SourceCodeEnricher(
                    fprHandle, SourceDecoders.fromToken("UTF-8"), null)
                    .enrichWithSourceCodeDetailed(List.of(List.of(
                            element("good.java"),
                            element("bad-one.java"),
                            element("bad-two.java"),
                            element("bad-one.java"))));

            assertTrue(result.hasFailures());
            assertEquals(List.of("bad-one.java", "bad-two.java"),
                    result.failures().stream().map(SourceCodeEnricher.SourceFileFailure::filename).toList());
            assertEquals(List.of("good.java"), result.files().keySet().stream().toList());
        }
    }

    private static StackTraceElement element(String filename) {
        return new StackTraceElement(filename, 1, "", "", null, null, null);
    }

    private Path createFpr(Map<String, byte[]> sourceFiles) throws IOException {
        Path fprPath = tempDir.resolve("source-enricher-test.fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zip, "src-archive/index.xml", createIndex(sourceFiles.keySet()).getBytes(StandardCharsets.UTF_8));
            int index = 1;
            for (Map.Entry<String, byte[]> sourceFile : sourceFiles.entrySet()) {
                writeEntry(zip, "src-archive/" + index++, sourceFile.getValue());
            }
        }
        return fprPath;
    }

    private static String createIndex(Iterable<String> filenames) {
        StringBuilder index = new StringBuilder("<?xml version='1.0' encoding='utf8'?><properties>");
        int entry = 1;
        for (String filename : filenames) {
            index.append("<entry key=\"").append(filename).append("\">src-archive/")
                    .append(entry++).append("</entry>");
        }
        return index.append("</properties>").toString();
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}