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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

class AviatorSSCUploadFailureMessageTest {
    private static final String AMBIGUOUS_MESSAGE = "Audit finished, but fcli could not confirm the upload to SSC. "
        + "Check the artifact history before retrying. Quota may have been consumed.";

    @TempDir Path tempDir;

    @ParameterizedTest
    @CsvSource({
        "SAST, Invalid JSON, Invalid JSON",
        "DAST, Invalid JSON, Invalid JSON",
        "SAST, , IOException",
        "DAST, '   ', IOException"
    })
    void ambiguousUploadPreservesContextAndDeepestCause(
            AviatorSSCFprFilterHelper.Engine engine, String rootMessage, String expectedRoot) throws Exception {
        Path fpr = createFpr();
        var root = new IOException(rootMessage);
        var failure = new FcliTechnicalException("Upload response parsing failed",
            new IOException("intermediate failure", root));
        try (var progress = ProgressWriterType.none.create()) {
            var upload = AviatorSSCFprTransferHelper.uploadEngineSpecificFprBestEffort(
                fpr, engine, new AviatorLoggerImpl(progress),
                candidate -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(candidate, engine),
                candidate -> { throw failure; });

            assertNull(upload.artifactId());
            assertAll(
                () -> assertTrue(upload.failureMessage().startsWith(AMBIGUOUS_MESSAGE), upload.failureMessage()),
                () -> assertTrue(upload.failureMessage().contains("Cause: Upload response parsing failed"), upload.failureMessage()),
                () -> assertTrue(upload.failureMessage().contains("Root cause: " + expectedRoot), upload.failureMessage()),
                () -> assertFalse(upload.failureMessage().toLowerCase().contains("upload failed"), upload.failureMessage()),
                () -> assertFalse(upload.failureMessage().contains("SSC rejected"), upload.failureMessage()));
            var result = JsonHelper.getObjectMapper().createObjectNode();
            result.putObject("operation").putObject("audit").put("message", "Completed audit detail");

            AviatorSSCAuditHelper.setUploadResult(result, upload);

            assertEquals("AUDITED_NOT_UPLOADED", result.path("__action__").asText());
            assertEquals(1, AviatorSSCAuditHelper.getAuditExitCode(result));
            String finalMessage = result.path("operation").path("audit").path("message").asText();
            assertTrue(finalMessage.contains(upload.failureMessage()), finalMessage);
            assertTrue(finalMessage.contains("Completed audit detail"), finalMessage);
        }
    }

    @Test
    void uploadFailureGuidanceIsAvailableInTheMessageBundle() {
        var messages = ResourceBundle.getBundle("com.fortify.cli.aviator.i18n.AviatorMessages");
        var values = messages.keySet().stream().map(messages::getString).toList();

        assertAll(
            () -> assertTrue(values.stream().anyMatch(value -> value.contains(AMBIGUOUS_MESSAGE)),
                "Externalize the approved ambiguous-outcome guidance, not just command help"),
            () -> assertTrue(values.stream().anyMatch(value -> value.contains("SSC rejected the upload request")),
                "Externalize the definitive-rejection guidance"),
            () -> assertTrue(values.stream().anyMatch(value -> value.contains("Root cause:")),
                "Externalize the new root-cause label"),
            () -> assertTrue(values.stream().anyMatch(value -> value.contains("Audit result:")),
                "Externalize the new upload/audit message composition"));
    }

    private Path createFpr() throws IOException {
        Path fpr = tempDir.resolve("audited.fpr");
        try (var zip = new ZipOutputStream(Files.newOutputStream(fpr))) {
            for (String entry : List.of("audit.fvdl", "audit.xml", "webinspect.xml")) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(entry.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return fpr;
    }
}
