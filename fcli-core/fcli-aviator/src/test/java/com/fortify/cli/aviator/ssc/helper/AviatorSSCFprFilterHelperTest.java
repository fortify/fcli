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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.common.exception.FcliSimpleException;

class AviatorSSCFprFilterHelperTest {
    @TempDir
    Path tempDir;

    @Test
    void filtersSastPayloadAndPreservesNestedEntriesAndSource() throws Exception {
        Path source = createFpr(Map.of(
            "audit.fvdl", "sast",
            "audit.xml", "audit",
            "sources/source.java", "source",
            "webinspect.xml", "dast"));
        byte[] sourceBytes = Files.readAllBytes(source);

        Path filtered = AviatorSSCFprFilterHelper.createEngineSpecificFpr(
            source, AviatorSSCFprFilterHelper.Engine.SAST, tempDir);

        assertEquals(Set.of("audit.fvdl", "audit.xml", "sources/source.java"), entries(filtered));
        assertArrayEquals(sourceBytes, Files.readAllBytes(source));
        Files.delete(filtered);
    }

    @Test
    void filtersDastToKnownPayload() throws Exception {
        Path source = createFpr(Map.of(
            "VERSION", "1",
            "webinspect.xml", "dast",
            "audit.xml", "audit",
            "attachments.xml", "attachments",
            "audit.fvdl", "sast",
            "sources/source.java", "source"));

        Path filtered = AviatorSSCFprFilterHelper.createEngineSpecificFpr(
            source, AviatorSSCFprFilterHelper.Engine.DAST, tempDir);

        assertEquals(Set.of("VERSION", "webinspect.xml", "audit.xml", "attachments.xml"), entries(filtered));
        Files.delete(filtered);
    }

    @Test
    void rejectsMissingSastPayloadAndDeletesOutput() throws Exception {
        Path source = createFpr(Map.of("audit.xml", "audit"));
        Set<Path> temporaryFilesBefore = filteredFprTempFiles(tempDir);

        assertThrows(FcliSimpleException.class, () -> AviatorSSCFprFilterHelper.createEngineSpecificFpr(
            source, AviatorSSCFprFilterHelper.Engine.SAST, tempDir));
        assertEquals(temporaryFilesBefore, filteredFprTempFiles(tempDir));
    }

    @Test
    void rejectsMissingDastPayloadAndDeletesOutput() throws Exception {
        Path source = createFpr(Map.of("VERSION", "1", "audit.xml", "audit"));
        Set<Path> temporaryFilesBefore = filteredFprTempFiles(tempDir);

        assertThrows(FcliSimpleException.class, () -> AviatorSSCFprFilterHelper.createEngineSpecificFpr(
            source, AviatorSSCFprFilterHelper.Engine.DAST, tempDir));
        assertEquals(temporaryFilesBefore, filteredFprTempFiles(tempDir));
    }

    private Path createFpr(Map<String, String> entries) throws Exception {
        long fileCount;
        try (var files = Files.list(tempDir)) {
            fileCount = files.count();
        }
        Path fpr = tempDir.resolve("source-" + fileCount + ".fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fpr))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes());
                zip.closeEntry();
            }
        }
        return fpr;
    }

    private static Set<String> entries(Path fpr) throws Exception {
        try (var fileSystem = java.nio.file.FileSystems.newFileSystem(fpr, (ClassLoader) null)) {
            try (var paths = Files.walk(fileSystem.getPath("/"))) {
                return paths.filter(path -> !Files.isDirectory(path))
                    .map(path -> fileSystem.getPath("/").relativize(path).toString().replace('\\', '/'))
                    .collect(java.util.stream.Collectors.toSet());
            }
        }
    }

    private static Set<Path> filteredFprTempFiles(Path tempDir) throws Exception {
        try (var paths = Files.list(tempDir)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("aviator_")
                    && path.getFileName().toString().endsWith(".fpr"))
                .collect(java.util.stream.Collectors.toSet());
        }
    }
}
