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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;

class AviatorSSCFprTransferHelperTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsUploadedArtifactId() throws Exception {
        var response = JsonHelper.getObjectMapper().readTree("{\"data\":{\"id\":2786}}");

        assertEquals("2786", AviatorSSCFprTransferHelper.getUploadedArtifactId(response));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "{}", "{\"data\":{}}", "{\"data\":{\"id\":null}}",
        "{\"data\":{\"id\":\"\"}}", "{\"data\":{\"id\":\"   \"}}"
    })
    void detectsMissingArtifactIdAsUploadFailure(String responseJson) throws Exception {
        var response = responseJson == null ? null : JsonHelper.getObjectMapper().readTree(responseJson);

        var failure = assertThrows(FcliTechnicalException.class,
            () -> AviatorSSCFprTransferHelper.getUploadedArtifactId(response));

        assertEquals("SSC FPR upload response did not contain an artifact ID", failure.getMessage());
    }

    @ParameterizedTest
    @EnumSource(AviatorSSCFprFilterHelper.Engine.class)
    void pruningFailureUploadsUnchangedFullOriginal(AviatorSSCFprFilterHelper.Engine engine) throws Exception {
        Path fpr = createFpr();
        byte[] originalBytes = Files.readAllBytes(fpr);
        var logger = new RecordingLogger();

        String artifactId = AviatorSSCFprTransferHelper.uploadEngineSpecificFprBestEffort(
            fpr,
            engine,
            logger,
            candidate -> {
                try (var fileSystem = FileSystems.newFileSystem(candidate, (ClassLoader) null)) {
                    Files.delete(fileSystem.getPath(
                        engine == AviatorSSCFprFilterHelper.Engine.SAST ? "/webinspect.xml" : "/audit.fvdl"));
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
                throw new IllegalStateException("simulated partial prune failure");
            },
            candidate -> {
                assertEquals(fpr, candidate);
                assertArrayEquals(originalBytes, readBytes(candidate));
                return "2786";
            });

        assertEquals("2786", artifactId);
        assertArrayEquals(originalBytes, Files.readAllBytes(fpr));
        assertEquals(Set.of(fpr), fprFiles());
        assertTrue(logger.messages.stream().anyMatch(message ->
            message.contains("uploading the full original FPR including all engines")));
    }

    @Test
    void successfulPruningUploadsEngineOnlyCandidate() throws Exception {
        Path fpr = createFpr();
        byte[] originalBytes = Files.readAllBytes(fpr);
        var logger = new RecordingLogger();
        var uploadedEntries = new ArrayList<Set<String>>();

        String artifactId = AviatorSSCFprTransferHelper.uploadEngineSpecificFprBestEffort(
            fpr,
            AviatorSSCFprFilterHelper.Engine.SAST,
            logger,
            candidate -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
                candidate, AviatorSSCFprFilterHelper.Engine.SAST),
            candidate -> {
                assertTrue(!fpr.equals(candidate));
                uploadedEntries.add(entries(candidate));
                return "2786";
            });

        assertEquals("2786", artifactId);
        assertEquals(List.of(Set.of("VERSION", "attachments.xml", "audit.fvdl", "audit.xml")), uploadedEntries);
        assertArrayEquals(originalBytes, Files.readAllBytes(fpr));
        assertEquals(Set.of(fpr), fprFiles());
        assertTrue(logger.messages.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(AviatorSSCFprFilterHelper.Engine.class)
    void uploadFailureReturnsNullAndCleansCandidate(AviatorSSCFprFilterHelper.Engine engine) throws Exception {
        Path fpr = createFpr();
        byte[] originalBytes = Files.readAllBytes(fpr);
        var logger = new RecordingLogger();

        String artifactId = AviatorSSCFprTransferHelper.uploadEngineSpecificFprBestEffort(
            fpr,
            engine,
            logger,
            candidate -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
                candidate, engine),
            candidate -> {
                assertEquals(Set.of("VERSION", "audit.xml", "attachments.xml",
                    engine == AviatorSSCFprFilterHelper.Engine.SAST ? "audit.fvdl" : "webinspect.xml"), entries(candidate));
                throw new FcliTechnicalException("simulated upload failure");
            });

        assertNull(artifactId);
        assertArrayEquals(originalBytes, Files.readAllBytes(fpr));
        assertEquals(Set.of(fpr), fprFiles());
        assertTrue(logger.messages.stream().anyMatch(message ->
            message.equals("WARN: Upload of audited " + engine + " FPR to SSC failed: simulated upload failure")));
    }

    private Path createFpr() throws IOException {
        Path fpr = Files.createTempFile(tempDir, "source-", ".fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fpr))) {
            for (String entry : List.of("audit.fvdl", "audit.xml", "webinspect.xml", "VERSION", "attachments.xml")) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(entry.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return fpr;
    }

    private static Set<String> entries(Path fpr) {
        try (var fileSystem = FileSystems.newFileSystem(fpr, (ClassLoader) null);
                var paths = Files.walk(fileSystem.getPath("/"))) {
            return paths.filter(path -> !Files.isDirectory(path))
                .map(path -> fileSystem.getPath("/").relativize(path).toString().replace('\\', '/'))
                .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private Set<Path> fprFiles() throws IOException {
        try (var paths = Files.list(tempDir)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".fpr"))
                .collect(Collectors.toSet());
        }
    }

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static final class RecordingLogger implements IAviatorLogger {
        private final List<String> messages = new ArrayList<>();

        @Override public void progress(String format, Object... args) { messages.add(String.format(format, args)); }
        @Override public void info(String format, Object... args) {}
        @Override public void warn(String format, Object... args) {}
        @Override public void error(String format, Object... args) {}
    }
}
