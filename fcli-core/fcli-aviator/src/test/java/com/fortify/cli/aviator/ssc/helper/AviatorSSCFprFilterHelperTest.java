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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.common.exception.FcliSimpleException;

class AviatorSSCFprFilterHelperTest {
    @TempDir
    Path tempDir;

    @Test
    void filtersSastFprInPlaceAndPreservesNestedEntries() throws Exception {
        Path fpr = createFpr(Map.of(
            "audit.fvdl", "sast",
            "audit.xml", "audit",
            "sources/source.java", "source",
            "webinspect.xml", "root dast",
            "nested/webinspect.xml", "nested"));

        AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
            fpr, AviatorSSCFprFilterHelper.Engine.SAST);

        assertEquals(Set.of("audit.fvdl", "audit.xml", "sources/source.java", "nested/webinspect.xml"),
            entries(fpr));
        assertEquals("sast", entryContent(fpr, "/audit.fvdl"));
        assertEquals("nested", entryContent(fpr, "/nested/webinspect.xml"));
        assertEquals(Set.of(fpr), fprFiles());
    }

    @Test
    void filtersDastFprInPlaceToAcceptedRootPayload() throws Exception {
        Path fpr = createFpr(Map.of(
            "VERSION", "1",
            "webinspect.xml", "dast",
            "audit.xml", "audit",
            "attachments.xml", "attachments",
            "audit.fvdl", "sast",
            "sources/source.java", "source",
            "nested/webinspect.xml", "nested dast",
            "nested/deeper/audit.xml", "nested audit"));

        AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
            fpr, AviatorSSCFprFilterHelper.Engine.DAST);

        assertEquals(Set.of("VERSION", "webinspect.xml", "audit.xml", "attachments.xml"), entries(fpr));
        assertEquals("dast", entryContent(fpr, "/webinspect.xml"));
        assertEquals(Set.of(fpr), fprFiles());
    }

    @Test
    void removesDastDirectoriesNamedLikeKeptFiles() throws Exception {
        Path fpr = createFpr(Map.of(
            "webinspect.xml", "dast",
            "VERSION/nested/file.txt", "not a root regular file",
            "audit.xml/nested/file.txt", "not a root regular file",
            "attachments.xml/nested/file.txt", "not a root regular file"));

        AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
            fpr, AviatorSSCFprFilterHelper.Engine.DAST);

        assertEquals(Set.of("webinspect.xml"), entries(fpr));
        try (var fileSystem = FileSystems.newFileSystem(fpr, (ClassLoader) null);
                var paths = Files.walk(fileSystem.getPath("/"))) {
            assertEquals(2, paths.count());
        }
        assertEquals(Set.of(fpr), fprFiles());
    }

    @Test
    void rejectsMissingSastPayloadWithoutChangingOrLeakingTheFpr() throws Exception {
        Path fpr = createFpr(Map.of("audit.xml", "audit", "webinspect.xml", "dast"));
        Set<String> entriesBefore = entries(fpr);

        assertThrows(FcliSimpleException.class, () -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
            fpr, AviatorSSCFprFilterHelper.Engine.SAST));

        assertEquals(entriesBefore, entries(fpr));
        assertEquals(Set.of(fpr), fprFiles());
    }

    @Test
    void rejectsMissingDastPayloadWithoutChangingOrLeakingTheFpr() throws Exception {
        Path fpr = createFpr(Map.of("VERSION", "1", "audit.xml", "audit"));
        Set<String> entriesBefore = entries(fpr);

        assertThrows(FcliSimpleException.class, () -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
            fpr, AviatorSSCFprFilterHelper.Engine.DAST));

        assertEquals(entriesBefore, entries(fpr));
        assertEquals(Set.of(fpr), fprFiles());
    }

    private Path createFpr(Map<String, String> entries) throws Exception {
        Path fpr = Files.createTempFile(tempDir, "source-", ".fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fpr))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return fpr;
    }

    private static Set<String> entries(Path fpr) throws Exception {
        try (var fileSystem = FileSystems.newFileSystem(fpr, (ClassLoader) null)) {
            try (var paths = Files.walk(fileSystem.getPath("/"))) {
                return paths.filter(path -> !Files.isDirectory(path))
                    .map(path -> fileSystem.getPath("/").relativize(path).toString().replace('\\', '/'))
                    .collect(Collectors.toSet());
            }
        }
    }

    private static String entryContent(Path fpr, String entry) throws Exception {
        try (var fileSystem = FileSystems.newFileSystem(fpr, (ClassLoader) null)) {
            return Files.readString(fileSystem.getPath(entry), StandardCharsets.UTF_8);
        }
    }

    private Set<Path> fprFiles() throws Exception {
        try (var paths = Files.list(tempDir)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".fpr"))
                .collect(Collectors.toSet());
        }
    }
}
