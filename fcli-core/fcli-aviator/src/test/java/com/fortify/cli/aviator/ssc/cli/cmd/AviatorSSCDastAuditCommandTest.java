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
package com.fortify.cli.aviator.ssc.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.config.AviatorConfigManager;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator.audit.DastAuditFprResult;
import com.fortify.cli.aviator.audit.DastAuditFprStatus;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import picocli.CommandLine;

class AviatorSSCDastAuditCommandTest {
    @TempDir Path tempDir;

    @Test
    void rejectsClientSideToneImprovementOptions() {
        assertThrows(CommandLine.ParameterException.class, () -> parse("--improve-tone"));
        assertThrows(CommandLine.ParameterException.class, () -> parse("--no-improve-tone"));
    }

    @Test
    void acceptsCustomTagMapping() {
        var commandLine = new CommandLine(new AviatorSSCDastAuditCommand());

        var parseResult = commandLine.parseArgs("--av", "test:1.0", "--tag-mapping", "dast-tags.yaml");

        assertEquals("dast-tags.yaml", parseResult.matchedOptionValue("--tag-mapping", null));
    }

    @Test
    void acceptsSscRefreshOptions() {
        var commandLine = new CommandLine(new AviatorSSCDastAuditCommand());

        var parseResult = commandLine.parseArgs(
            "--av", "test:1.0", "--no-refresh", "--refresh-timeout", "2m");

        assertFalse(parseResult.matchedOptionValue("--refresh", true));
        assertEquals("2m", parseResult.matchedOptionValue("--refresh-timeout", null));
    }

    @Test
    void dastAuditStatsAreReturnedWithoutArtifactId() {
        var appVersion = new SSCAppVersionDescriptor();
        appVersion.setVersionId("42");
        appVersion.setApplicationName("WebGoat");
        appVersion.setVersionName("1.0");
        var auditResult = DastAuditFprResult.builder()
            .status(DastAuditFprStatus.PARTIALLY_AUDITED)
            .totalReported(8)
            .eligible(6)
            .submitted(6)
            .succeeded(4)
            .truePositives(2)
            .falsePositivesSuppressed(1)
            .likelyFalsePositives(1)
            .skipped(2)
            .reservedQuota(6)
            .exceededCount(2)
            .build();

        var result = new AviatorSSCDastAuditCommand().buildOutput(appVersion, auditResult, null);

        assertEquals("42", result.path("id").asText());
        assertEquals("WebGoat", result.path("applicationName").asText());
        assertTrue(result.path("artifactId").isMissingNode() || result.path("artifactId").isNull());
        assertEquals("PARTIALLY_AUDITED", result.path("__action__").asText());
        assertEquals(6, result.path("operation").path("audit").path("submitted").asInt());
        assertEquals(4, result.path("operation").path("audit").path("succeeded").asInt());
        assertEquals(2, result.path("operation").path("audit").path("skipped").asInt());
        assertEquals(0, result.path("operation").path("audit").path("failed").asInt());
        assertFalse(result.path("operation").path("audit").has("truePositives"));
        assertFalse(result.path("operation").path("audit").has("falsePositivesSuppressed"));
        assertFalse(result.path("operation").path("audit").has("likelyFalsePositives"));
        assertFalse(result.has("state"));
        assertFalse(result.has("submitted"));
    }

    @ParameterizedTest
    @CsvSource({
        "AUDITED, true, 4, missing-id, 0",
        "PARTIALLY_AUDITED, true, 4, missing-id, 0",
        "AUDITED, true, 4, http-error, 0",
        "AUDITED, true, 4, none, 1",
        "PARTIALLY_AUDITED, true, 4, none, 0",
        "SKIPPED, false, 0, none, 1",
        "AUDITED, false, 4, none, 1",
        "AUDITED, true, 0, none, 1",
        "PARTIALLY_AUDITED, false, 0, none, 0",
        "FAILED, false, 0, none, 0"
    })
    void timestampsOnlyPublishedOrUnchangedCompletedAudits(
            DastAuditFprStatus status, boolean updated, int succeeded, String uploadFailure, int expectedWrites)
            throws Exception {
        Path fpr = tempDir.resolve("audited.fpr");
        try (var zip = new ZipOutputStream(Files.newOutputStream(fpr))) {
            for (String entry : List.of("audit.fvdl", "webinspect.xml")) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(entry.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        var auditResult = DastAuditFprResult.builder()
            .status(status).updatedFile(updated ? fpr.toFile() : null)
            .totalReported(8).eligible(6).submitted(6).succeeded(succeeded).skipped(6 - succeeded)
            .reservedQuota(6).exceededCount(2).build();
        var command = new CompletedAuditCommand(auditResult);
        var appVersion = new SSCAppVersionDescriptor();
        appVersion.setVersionId("42");
        appVersion.setApplicationName("WebGoat");
        appVersion.setVersionName("1.0");
        var progress = new RecordingProgressWriter();
        var uploads = new AtomicInteger();
        var validations = new AtomicInteger();
        var writes = new AtomicInteger();
        var timestampUpdate = new AtomicReference<JsonNode>();
        var uploadedBody = new AtomicReference<String>();
        var candidatesAtValidation = new AtomicInteger(-1);
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/projectVersions/42/customTags", exchange -> {
            validations.incrementAndGet();
            try (var paths = Files.list(tempDir)) {
                candidatesAtValidation.set((int) paths.count() - 1);
            }
            respond(exchange, 200, "{}");
        });
        server.createContext("/api/v1/projectVersions/42/artifacts", exchange -> {
            uploads.incrementAndGet();
            uploadedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "http-error".equals(uploadFailure) ? 500 : 200,
                "missing-id".equals(uploadFailure) ? "{\"data\":{}}" : "{\"data\":{\"id\":\"2786\"}}");
        });
        server.createContext("/api/v1/attributeDefinitions", exchange -> respond(exchange, 200, """
            {"data":[{"id":"42","guid":"C3D4E5F6-A7B8-9012-BCDE-F12345678902","name":"last_dast_audit",
            "category":"TECHNICAL","type":"TEXT","required":false,"hasDefault":false,"options":[]}]}
            """));
        server.createContext("/api/v1/projectVersions/42/attributes", exchange -> {
            if (!"PUT".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "{}");
                return;
            }
            writes.incrementAndGet();
            timestampUpdate.set(JsonHelper.getObjectMapper().readTree(exchange.getRequestBody()));
            respond(exchange, 200, "{}");
        });
        server.start();
        try (var unirest = UnirestHelper.createUnirestInstance(instance -> {
            UnirestJsonHeaderConfigurer.configure(instance);
            UnirestUnexpectedHttpResponseConfigurer.configure(instance);
            instance.config().defaultBaseUrl("http://localhost:" + server.getAddress().getPort());
        })) {
            var tagMapping = AviatorConfigManager.getInstance().getDefaultDastTagMappingConfig().resolveForDast();
            var result = command.processFpr(unirest, appVersion, fpr, new AviatorUserSessionDescriptor(),
                new AviatorLoggerImpl(progress), tagMapping, progress);

            boolean uploadRequired = updated && succeeded > 0;
            boolean uploadSucceeded = uploadRequired && "none".equals(uploadFailure);
            assertEquals(status.name(), result.path("__action__").asText());
            assertEquals(status.name(), command.getActionCommandResult());
            assertEquals("42", result.path("id").asText());
            assertEquals(6, result.path("operation").path("audit").path("submitted").asInt());
            assertEquals(succeeded, result.path("operation").path("audit").path("succeeded").asInt());
            assertEquals(6 - succeeded, result.path("operation").path("audit").path("skipped").asInt());
            assertEquals(0, result.path("operation").path("audit").path("failed").asInt());
            if (uploadSucceeded) {
                assertEquals("2786", result.path("artifactId").asText());
            } else {
                assertTrue(result.path("artifactId").isNull());
            }
            assertEquals(uploadRequired ? 1 : 0, uploads.get());
            assertEquals(uploadRequired ? 1 : 0, validations.get());
            assertEquals(expectedWrites, writes.get());
            if (expectedWrites > 0) {
                assertEquals("42", timestampUpdate.get().get(0).path("attributeDefinitionId").asText());
                assertTrue(timestampUpdate.get().get(0).path("value").asText().matches("\\d{4}-\\d{2}-\\d{2}T.*Z"));
            }
            if (uploadRequired) {
                assertEquals(0, candidatesAtValidation.get(), "Validate tags before candidate creation/pruning");
                assertTrue(uploadedBody.get().contains("webinspect.xml"));
                assertTrue(uploadedBody.get().contains("audit.xml"));
                assertFalse(uploadedBody.get().contains("audit.fvdl"));
                if (!uploadSucceeded) {
                    assertTrue(progress.messages.stream().anyMatch(message ->
                        message.startsWith("WARN: Upload of audited DAST FPR to SSC failed:")));
                }
            }
            try (var handle = new FprHandle(fpr)) {
                assertTrue(Files.exists(handle.getPath("/audit.fvdl")));
                assertTrue(Files.exists(handle.getPath("/webinspect.xml")));
                assertEquals("updated audit", Files.readString(handle.getPath("/audit.xml")));
            }
            try (var paths = Files.list(tempDir)) {
                assertEquals(List.of(fpr), paths.toList());
            }
        } finally {
            server.stop(0);
        }
    }

    private static final class CompletedAuditCommand extends AviatorSSCDastAuditCommand {
        private final DastAuditFprResult result;

        private CompletedAuditCommand(DastAuditFprResult result) {
            this.result = result;
        }

        @Override
        DastAuditFprResult auditFpr(Path fpr, SSCAppVersionDescriptor appVersion,
                AviatorUserSessionDescriptor session, IAviatorLogger logger, TagMappingConfig tagMappingConfig)
                throws Exception {
            try (var handle = new FprHandle(fpr)) {
                Files.writeString(handle.getPath("/audit.xml"), "updated audit");
            }
            return result;
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private static final class RecordingProgressWriter implements IProgressWriter {
        private final List<String> messages = new ArrayList<>();
        @Override public boolean isMultiLineSupported() { return false; }
        @Override public void writeProgress(String message, Object... args) { messages.add(String.format(message, args)); }
        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) {}
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void clearProgress() {}
        @Override public void close() {}
        @Override public String type() { return "test"; }
    }

    private static AviatorSSCDastAuditCommand parse(String... args) {
        var command = new AviatorSSCDastAuditCommand();
        var fullArgs = new ArrayList<String>();
        Collections.addAll(fullArgs, "--av", "test:1.0");
        Collections.addAll(fullArgs, args);
        new CommandLine(command).parseArgs(fullArgs.toArray(String[]::new));
        return command;
    }
}