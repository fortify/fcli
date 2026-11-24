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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.ProgressWriterType;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.sun.net.httpserver.HttpServer;

class AviatorSSCFprTransferHelperTest {
    private static final String AMBIGUOUS_MESSAGE = "Audit finished, but fcli could not confirm the upload to SSC. "
        + "Check the artifact history before retrying. Quota may have been consumed.";

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @CsvSource({
        ",",
        "2786, Upload not confirmed",
        "'',",
        "'   ',",
        ", ''",
        ", '   '"
    })
    void rejectsInvalidUploadResults(String artifactId, String failureMessage) {
        assertThrows(FcliBugException.class,
            () -> new AviatorSSCFprTransferHelper.UploadResult(artifactId, failureMessage));
    }

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
    @ValueSource(strings = {"none", "http-error", "missing-id"})
    void correlationTransferStillUploadsFullFprAndPropagatesFailures(String failure) throws Exception {
        Path fpr = createFpr();
        byte[] originalBytes = Files.readAllBytes(fpr);
        var uploadedBody = new AtomicReference<String>();
        var appVersion = new SSCAppVersionDescriptor();
        appVersion.setVersionId("42");
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/projectVersions/42/artifacts", exchange -> {
            uploadedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            byte[] response = ("missing-id".equals(failure) ? "{\"data\":{}}" : "{\"data\":{\"id\":\"2786\"}}")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders("http-error".equals(failure) ? 500 : 200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try (var unirest = UnirestHelper.createUnirestInstance(instance -> {
            UnirestJsonHeaderConfigurer.configure(instance);
            UnirestUnexpectedHttpResponseConfigurer.configure(instance);
            instance.config().defaultBaseUrl("http://localhost:" + server.getAddress().getPort());
        }); var progress = ProgressWriterType.none.create()) {
            if ("none".equals(failure)) {
                assertEquals("2786", AviatorSSCFprTransferHelper.uploadFpr(unirest, appVersion, fpr, progress));
            } else if ("http-error".equals(failure)) {
                var exception = assertThrows(UnexpectedHttpResponseException.class,
                    () -> AviatorSSCFprTransferHelper.uploadFpr(unirest, appVersion, fpr, progress));
                assertEquals(500, exception.getStatus());
            } else {
                var exception = assertThrows(FcliTechnicalException.class,
                    () -> AviatorSSCFprTransferHelper.uploadFpr(unirest, appVersion, fpr, progress));
                assertEquals("SSC FPR upload response did not contain an artifact ID", exception.getMessage());
            }
            assertTrue(uploadedBody.get().contains(new String(originalBytes, StandardCharsets.ISO_8859_1)),
                "The transfer used by correlation must upload the untouched mixed-engine FPR");
            assertArrayEquals(originalBytes, Files.readAllBytes(fpr));
            assertEquals(Set.of(fpr), fprFiles());
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @CsvSource({"SAST, false", "DAST, false", "SAST, true", "DAST, true"})
    void pruningFailureUploadsUnchangedFullOriginalAndCleansCandidate(
            AviatorSSCFprFilterHelper.Engine engine, boolean uploadFails) throws Exception {
        Path fpr = createFpr();
        byte[] originalBytes = Files.readAllBytes(fpr);
        var logger = new RecordingLogger();

        var result = AviatorSSCFprTransferHelper.uploadEngineSpecificFprBestEffort(
            fpr,
            "42",
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
                if (uploadFails) {
                    throw new FcliTechnicalException("simulated fallback upload failure");
                }
                return "2786";
            });

        if (uploadFails) {
            assertNull(result.artifactId());
            assertTrue(result.failureMessage().startsWith(AMBIGUOUS_MESSAGE));
            assertTrue(result.failureMessage().endsWith("Cause: simulated fallback upload failure"));
        } else {
            assertEquals("2786", result.artifactId());
            assertNull(result.failureMessage());
        }
        assertArrayEquals(originalBytes, Files.readAllBytes(fpr));
        assertEquals(Set.of(fpr), fprFiles());
        assertTrue(logger.messages.stream().anyMatch(message ->
            message.contains("uploading the full original FPR including all engines")));
    }

    @ParameterizedTest
    @EnumSource(AviatorSSCFprFilterHelper.Engine.class)
    void successfulPruningUploadsEngineOnlyCandidate(AviatorSSCFprFilterHelper.Engine engine) throws Exception {
        Path fpr = createFpr();
        byte[] originalBytes = Files.readAllBytes(fpr);
        var logger = new RecordingLogger();
        var uploadedEntries = new ArrayList<Set<String>>();

        var result = AviatorSSCFprTransferHelper.uploadEngineSpecificFprBestEffort(
            fpr,
            "42",
            engine,
            logger,
            candidate -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
                candidate, engine),
            candidate -> {
                assertTrue(!fpr.equals(candidate));
                assertTrue(candidate.getFileName().toString().startsWith("aviator_42_"));
                assertTrue(candidate.getFileName().toString().endsWith(".fpr"));
                uploadedEntries.add(entries(candidate));
                return "2786";
            });

        assertEquals("2786", result.artifactId());
        assertNull(result.failureMessage());
        assertEquals(List.of(Set.of("VERSION", "attachments.xml", "audit.xml",
            engine == AviatorSSCFprFilterHelper.Engine.SAST ? "audit.fvdl" : "webinspect.xml")), uploadedEntries);
        assertArrayEquals(originalBytes, Files.readAllBytes(fpr));
        assertEquals(Set.of(fpr), fprFiles());
        assertTrue(logger.messages.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(AviatorSSCFprFilterHelper.Engine.class)
    void uploadFailureReturnsUnconfirmedResultAndCleansCandidate(AviatorSSCFprFilterHelper.Engine engine) throws Exception {
        Path fpr = createFpr();
        byte[] originalBytes = Files.readAllBytes(fpr);
        var logger = new RecordingLogger();

        var result = AviatorSSCFprTransferHelper.uploadEngineSpecificFprBestEffort(
            fpr,
            "42",
            engine,
            logger,
            candidate -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
                candidate, engine),
            candidate -> {
                assertEquals(Set.of("VERSION", "audit.xml", "attachments.xml",
                    engine == AviatorSSCFprFilterHelper.Engine.SAST ? "audit.fvdl" : "webinspect.xml"), entries(candidate));
                throw new FcliTechnicalException("simulated upload failure");
            });

        assertNull(result.artifactId());
        assertTrue(result.failureMessage().startsWith(AMBIGUOUS_MESSAGE));
        assertTrue(result.failureMessage().contains("Check the artifact history before retrying."));
        assertTrue(result.failureMessage().contains("Quota may have been consumed"));
        assertTrue(result.failureMessage().contains("Retrying the audit may consume more quota"));
        assertTrue(result.failureMessage().endsWith("Cause: simulated upload failure"));
        assertArrayEquals(originalBytes, Files.readAllBytes(fpr));
        assertEquals(Set.of(fpr), fprFiles());
        assertEquals(List.of("WARN: Upload of audited " + engine + " FPR to SSC: " + result.failureMessage()),
            logger.messages);
    }

    @ParameterizedTest
    @CsvSource({
        "Upload response parsing failed, Invalid JSON, Root cause: Invalid JSON",
        "Upload response parsing failed, , Root cause: IOException",
        "Upload response parsing failed, '   ', Root cause: IOException",
        "Connection reset, Connection reset, ''"
    })
    void uploadWarningRetainsExceptionContextAndDistinctDeepestCause(
            String uploadMessage, String rootMessage, String expectedRoot) throws Exception {
        Path fpr = createFpr();
        var logger = new RecordingLogger();
        var failure = new FcliTechnicalException(uploadMessage,
            new IOException("intermediate failure", new IOException(rootMessage)));

        var result = AviatorSSCFprTransferHelper.uploadEngineSpecificFprBestEffort(
            fpr, "42", AviatorSSCFprFilterHelper.Engine.SAST, logger,
            candidate -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(
                candidate, AviatorSSCFprFilterHelper.Engine.SAST),
            candidate -> { throw failure; });

        assertNull(result.artifactId());
        String message = result.failureMessage();
        assertTrue(message.startsWith(AMBIGUOUS_MESSAGE));
        assertTrue(message.contains("Cause: " + uploadMessage));
        if (expectedRoot.isEmpty()) {
            assertFalse(message.contains("Root cause:"));
        } else {
            assertTrue(message.endsWith(expectedRoot), message);
        }
        assertEquals(List.of("WARN: Upload of audited SAST FPR to SSC: " + message), logger.messages);
        var output = JsonHelper.getObjectMapper().createObjectNode();
        output.putObject("operation").putObject("audit").put("message", "Audit partially completed");
        AviatorSSCAuditHelper.setUploadResult(output, result);
        assertEquals(message + ". Audit result: Audit partially completed",
            output.path("operation").path("audit").path("message").asText());
        assertEquals("AUDITED_NOT_UPLOADED", output.path("__action__").asText());
        assertEquals(1, AviatorSSCAuditHelper.getAuditExitCode(output));
        assertEquals(Set.of(fpr), fprFiles());
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
