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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator.audit.model.AuditFprOptions;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAuditHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCFprTransferHelper;
import com.fortify.cli.common.cli.mixin.ICommandAware;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.sun.net.httpserver.HttpServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import kong.unirest.UnirestInstance;
import picocli.CommandLine;

class AviatorSSCAuditCommandTest {
    @TempDir Path tempDir;

    @Test
    void registersCanonicalAndDeprecatedCommandNames() {
        assertEquals("audit-sast", new CommandLine(new AviatorSSCSastAuditCommand()).getCommandName());
        assertEquals("audit", new CommandLine(new AviatorSSCAuditCommand()).getCommandName());
    }

    @Test
    void canonicalAndDeprecatedCommandsShareUnannotatedBase() {
        assertEquals(AbstractAviatorSSCSastAuditCommand.class, AviatorSSCSastAuditCommand.class.getSuperclass());
        assertEquals(AbstractAviatorSSCSastAuditCommand.class, AviatorSSCAuditCommand.class.getSuperclass());
        assertFalse(AbstractAviatorSSCSastAuditCommand.class.isAnnotationPresent(CommandLine.Command.class));
    }

    @Test
    void deprecatedCommandHelpPointsToCanonicalCommand() {
        var messages = ResourceBundle.getBundle("com.fortify.cli.aviator.i18n.AviatorMessages");
        String header = messages.getString("fcli.aviator.ssc.audit.usage.header");
        String description = messages.getString("fcli.aviator.ssc.audit.usage.description");

        assertTrue(header.contains("(DEPRECATED)"));
        assertTrue(description.contains("fcli aviator ssc audit-sast"));
    }

    @Test
    void canonicalCommandAllowsFilterOptions() {
        var cmd = parseCanonical("--filterset", "Security Auditor View", "--no-filterset");
        assertEquals("Security Auditor View", cmd.getFilterSetTitleOrId());
        assertTrue(cmd.isNoFilterSet());
    }

    @Test
    void testAllowsCombinedFilterOptions() {
        var cmd = parse("--filterset", "Security Auditor View", "--no-filterset");
        assertEquals("Security Auditor View", cmd.getFilterSetTitleOrId());
        assertTrue(cmd.isNoFilterSet());
    }

    @Test
    void testAllowsFilterSetOption() {
        var cmd = parse("--filterset", "Security Auditor View");
        assertEquals("Security Auditor View", cmd.getFilterSetTitleOrId());
        assertFalse(cmd.isNoFilterSet());
    }

    @Test
    void testAllowsNoFilterSetOption() {
        var cmd = parse("--no-filterset");
        assertTrue(cmd.isNoFilterSet());
        assertNull(cmd.getFilterSetTitleOrId());
    }

    @Test
    void testAllowsForceReauditOption() throws Exception {
        var cmd = parse("--force-reaudit");
        Field field = AbstractAviatorSSCSastAuditCommand.class.getDeclaredField("forceReaudit");
        field.setAccessible(true);
        assertTrue((boolean) field.get(cmd));
    }

    @Test
    void canonicalAndDeprecatedCommandsAllowSourceEncodings() {
        parse("--source-encodings", "UTF-8");
        parseCanonical("--source-encodings", "UTF-8");
    }

    @Test
    void reportsDecodeSkippedIssuesAsNotSubmitted() {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.set("operation", JsonHelper.getObjectMapper().createObjectNode());
        var auditResult = new FPRAuditResult(null, "SKIPPED", "All 6 issues were skipped", 0, 6, 0, 6,
                Map.of("Source file decode failed", 6), 0, Map.of());

        AviatorSSCAuditHelper.setAuditStats(result, auditResult);

        var audit = result.path("operation").path("audit");
        assertEquals(0, audit.get("submitted").asInt());
        assertEquals(0, audit.get("succeeded").asInt());
        assertEquals(6, audit.get("skipped").asInt());
    }

    @Test
    void reportsServerSkippedIssuesAsSubmitted() {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.set("operation", JsonHelper.getObjectMapper().createObjectNode());
        var auditResult = new FPRAuditResult(null, "SKIPPED", "Skipped by Aviator", 0, 1, 1, 1,
                Map.of("Skipped by Aviator", 1), 0, Map.of());

        AviatorSSCAuditHelper.setAuditStats(result, auditResult);

        assertEquals(1, result.path("operation").path("audit").get("submitted").asInt());
    }

    @Test
    void excludesOnlyDecodeSkippedIssuesFromSubmittedCount() {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.set("operation", JsonHelper.getObjectMapper().createObjectNode());
        var auditResult = new FPRAuditResult(null, "PARTIALLY_AUDITED", null, 2, 4, 3, 2,
                Map.of("Source file decode failed", 1, "Skipped by Aviator", 1), 0, Map.of());

        AviatorSSCAuditHelper.setAuditStats(result, auditResult);

        var audit = result.path("operation").path("audit");
        assertEquals(3, audit.get("submitted").asInt());
        assertEquals(2, audit.get("succeeded").asInt());
        assertEquals(2, audit.get("skipped").asInt());
    }

    @ParameterizedTest
    @CsvSource({
        "true, AUDITED, missing-id", "false, AUDITED, missing-id",
        "true, PARTIALLY_AUDITED, missing-id", "false, PARTIALLY_AUDITED, missing-id",
        "true, AUDITED, http-error", "false, AUDITED, http-error",
        "true, PARTIALLY_AUDITED, http-error", "false, PARTIALLY_AUDITED, http-error",
        "true, AUDITED, rejected", "false, AUDITED, rejected",
        "true, AUDITED, filter", "false, AUDITED, filter",
        "true, PARTIALLY_AUDITED, filter", "false, PARTIALLY_AUDITED, filter",
        "true, AUDITED, none", "false, AUDITED, none",
        "true, SKIPPED, none", "false, SKIPPED, none",
        "true, FAILED, none", "false, FAILED, none"
    })
    void emitsCompletedAuditAndExitCodeForBothCommandNames(boolean canonical, String status, String failure) throws Exception {
        Path fpr = tempDir.resolve("audited.fpr");
        try (var zip = new ZipOutputStream(Files.newOutputStream(fpr))) {
            for (String entry : List.of("audit.fvdl", "webinspect.xml")) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(entry.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        int succeeded = switch (status) {
            case "AUDITED" -> 3;
            case "PARTIALLY_AUDITED" -> 2;
            default -> 0;
        };
        int skipped = 3 - succeeded;
        Map<String, Integer> skippedByReason = skipped == 0 ? Map.of() : Map.of("Skipped by Aviator", skipped);
        var auditResult = new FPRAuditResult(fpr.toFile(), status, "Audit finished", succeeded, 3, 3, skipped,
            skippedByReason, 1, Map.of("No remediation available", 1));
        var completedAudit = new CompletedAudit(auditResult, "filter".equals(failure));
        var av = new SSCAppVersionDescriptor();
        av.setVersionId("42");
        av.setApplicationName("TestApp");
        av.setVersionName("1.0");
        var progress = new RecordingProgressWriter();
        var uploads = new AtomicInteger();
        var uploadedBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/projectVersions/42/customTags", exchange -> {
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.createContext("/api/v1/projectVersions/42/artifacts", exchange -> {
            uploads.incrementAndGet();
            uploadedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            byte[] response = ("missing-id".equals(failure) ? "{\"data\":{}}" : "{\"data\":{\"id\":\"2786\"}}")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            int httpStatus = "http-error".equals(failure) ? 500 : "rejected".equals(failure) ? 400 : 200;
            exchange.sendResponseHeaders(httpStatus, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        var uploadLog = (Logger) LoggerFactory.getLogger(AviatorSSCFprTransferHelper.class);
        Level previousLevel = uploadLog.getLevel();
        var warnings = new ListAppender<ILoggingEvent>();
        warnings.start();
        uploadLog.setLevel(Level.WARN);
        uploadLog.addAppender(warnings);
        server.start();
        try (var unirest = UnirestHelper.createUnirestInstance(instance -> {
            UnirestJsonHeaderConfigurer.configure(instance);
            UnirestUnexpectedHttpResponseConfigurer.configure(instance);
            instance.config().defaultBaseUrl("http://localhost:" + server.getAddress().getPort());
        })) {
            var logger = new AviatorLoggerImpl(progress);
            AbstractAviatorSSCSastAuditCommand command = canonical ? new AviatorSSCSastAuditCommand() {
                @Override public UnirestInstance getUnirestInstance() { return unirest; }
                @Override public JsonNode getJsonNode(UnirestInstance instance) {
                    return processFpr(instance, av, "unused", "unused", logger, fpr);
                }
                @Override FPRAuditResult auditFpr(AuditFprOptions options) { return completedAudit.auditFpr(options); }
            } : new AviatorSSCAuditCommand() {
                @Override public UnirestInstance getUnirestInstance() { return unirest; }
                @Override public JsonNode getJsonNode(UnirestInstance instance) {
                    return processFpr(instance, av, "unused", "unused", logger, fpr);
                }
                @Override FPRAuditResult auditFpr(AuditFprOptions options) { return completedAudit.auditFpr(options); }
            };
            var commandLine = new CommandLine(command);
            commandLine.parseArgs("--av", "TestApp:1.0", "--progress", "none");
            FcliCommandSpecHelper.getAllUserObjectsStream(commandLine.getCommandSpec()).forEach(object -> {
                if (object instanceof ICommandAware commandAware) {
                    commandAware.setCommandSpec(commandLine.getCommandSpec());
                }
            });
            var records = new ArrayList<ObjectNode>();
            command.setRecordConsumer(records::add, true);
            boolean uploadRequired = List.of("AUDITED", "PARTIALLY_AUDITED").contains(status);
            boolean uploadFailed = uploadRequired && List.of("missing-id", "http-error", "rejected").contains(failure);

            assertEquals(uploadFailed ? 1 : 0, command.call());
            assertEquals(1, records.size(), "Emit the audit record even when the command exits nonzero");
            var result = records.get(0);
            assertEquals(uploadFailed ? "AUDITED_NOT_UPLOADED" : status,
                result.path(IActionCommandResultSupplier.actionFieldName).asText());
            assertEquals("AUDITED", command.getActionCommandResult(),
                "The default action supplier must not overwrite the explicit audit outcome");
            assertFalse(completedAudit.auditPayloadPath.getFileSystem().isOpen(),
                "Audit handle must close before filtering/upload");
            var audit = result.path("operation").path("audit");
            assertEquals(3, audit.path("submitted").asInt());
            assertEquals(succeeded, audit.path("succeeded").asInt());
            assertEquals(skipped, audit.path("skipped").asInt());
            assertEquals(JsonHelper.getObjectMapper().valueToTree(skippedByReason), audit.path("skippedByReason"));
            assertEquals(skipped == 0 ? "" : "Skipped by Aviator=" + skipped, audit.path("skippedReasons").asText());
            assertEquals(1, audit.path("remediationGenerationSkipped").asInt());
            assertEquals(1, audit.path("remediationGenerationSkippedByReason").path("No remediation available").asInt());
            assertEquals("No remediation available=1", audit.path("remediationGenerationSkippedReasons").asText());
            String auditMessage = "AUDITED".equals(status) ? "Audit completed successfully" : "Audit finished";
            String uploadMessage = "rejected".equals(failure)
                ? "Audit finished, but SSC rejected the upload request (HTTP 400)."
                : "Audit finished, but fcli could not confirm the upload to SSC.";
            if (uploadFailed) {
                assertTrue(audit.path("message").asText().startsWith(uploadMessage));
                assertTrue(audit.path("message").asText().endsWith(". Audit result: " + auditMessage));
            } else {
                assertEquals(auditMessage, audit.path("message").asText());
            }
            assertEquals(uploadRequired ? 1 : 0, uploads.get());
            if (!uploadRequired) {
                assertTrue(result.path("artifactId").isNull());
            } else if ("none".equals(failure)) {
                assertEquals("2786", result.path("artifactId").asText());
                assertFalse(progress.messages.stream().anyMatch(message -> message.startsWith("WARN: Upload")));
            } else if ("filter".equals(failure)) {
                assertEquals("2786", result.path("artifactId").asText());
                assertTrue(uploadedBody.get().contains(new String(Files.readAllBytes(fpr), StandardCharsets.ISO_8859_1)),
                    "Pruning failure must upload the byte-for-byte untouched full audited FPR");
                assertTrue(progress.messages.stream().anyMatch(message ->
                    message.startsWith("WARN: Could not prune SAST FPR; uploading the full original FPR including all engines")));
            } else {
                assertTrue(result.path("artifactId").isNull());
                String message = audit.path("message").asText();
                assertTrue(message.contains(uploadMessage));
                assertTrue(message.contains("Quota may have been consumed"));
                assertTrue(message.contains("Retrying the audit may consume more quota"));
                String cause = "missing-id".equals(failure)
                    ? "SSC FPR upload response did not contain an artifact ID"
                    : "HTTP " + ("rejected".equals(failure) ? 400 : 500);
                assertTrue(message.contains(cause), message);
                if ("rejected".equals(failure)) {
                    assertTrue(message.contains("SSC rejected the upload request (HTTP 400)"), message);
                } else {
                    assertFalse(message.contains("rejected"), message);
                    assertTrue(message.contains("Check the artifact history before retrying."), message);
                }
                assertTrue(progress.messages.stream().anyMatch(warning ->
                    warning.startsWith("WARN: Upload of audited SAST FPR to SSC:")
                        && warning.contains(uploadMessage) && warning.contains(cause)));
                assertTrue(warnings.list.stream().anyMatch(event -> event.getLevel() == Level.WARN
                    && event.getThrowableProxy() != null && event.getThrowableProxy().getMessage().contains(cause)),
                    "Upload warning must retain the exception cause");
            }
            if (uploadRequired && !"filter".equals(failure)) {
                assertTrue(uploadedBody.get().contains("audit.fvdl"));
                assertFalse(uploadedBody.get().contains("webinspect.xml"));
            }
            try (var handle = new FprHandle(fpr)) {
                assertTrue(Files.exists(handle.getPath("/webinspect.xml")));
                assertEquals("updated audit", Files.readString(handle.getPath("/audit.xml")));
            }
            try (var paths = Files.list(tempDir)) {
                assertEquals(List.of(fpr), paths.toList(), "Always clean the upload candidate");
            }
        } finally {
            server.stop(0);
            uploadLog.detachAppender(warnings);
            uploadLog.setLevel(previousLevel);
            warnings.stop();
        }
    }

    private static final class CompletedAudit {
        private final FPRAuditResult auditResult;
        private final boolean removeSastPayload;
        private Path auditPayloadPath;

        private CompletedAudit(FPRAuditResult auditResult, boolean removeSastPayload) {
            this.auditResult = auditResult;
            this.removeSastPayload = removeSastPayload;
        }

        FPRAuditResult auditFpr(AuditFprOptions options) {
            auditPayloadPath = options.getFprHandle().getPath("/audit.fvdl");
            assertTrue(auditPayloadPath.getFileSystem().isOpen());
            assertTrue(Files.exists(options.getFprHandle().getPath("/webinspect.xml")),
                "Audit the full current-state FPR before pruning an upload candidate");
            try {
                Files.writeString(options.getFprHandle().getPath("/audit.xml"), "updated audit");
                if (removeSastPayload) {
                    Files.delete(auditPayloadPath);
                }
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            return auditResult;
        }
    }

    @ParameterizedTest
    @CsvSource({
        "true, default, -1, 1", "false, default, -1, 1",
        "true, force, -1, 1", "false, force, -1, 1",
        "true, priority, -1, 1", "false, priority, -1, 1",
        "true, skip, 2, 2", "false, skip, 2, 2",
        "true, test, 2, 2", "false, test, 2, 2"
    })
    void preflightOnlyCountsAllPagesWhenQuotaOptionsRequireIt(boolean canonical, String mode, long expectedCount,
            int expectedRequests) throws Exception {
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/projectVersions/42/issues", exchange -> {
            requests.incrementAndGet();
            ObjectNode issue = JsonHelper.getObjectMapper().createObjectNode().put("audited", false);
            issue.putObject("_embed").putArray("auditValues");
            // Return one row per page to ensure exact counting follows SSC's page size.
            byte[] response = JsonHelper.getObjectMapper().createObjectNode().put("count", 2)
                    .set("data", JsonHelper.getObjectMapper().createArrayNode().add(issue))
                    .toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try (var unirest = UnirestHelper.createUnirestInstance(instance -> {
            UnirestJsonHeaderConfigurer.configure(instance);
            instance.config().defaultBaseUrl("http://localhost:" + server.getAddress().getPort());
        })) {
            var args = new ArrayList<String>();
            args.add("--no-filterset");
            switch (mode) {
                case "force" -> args.add("--force-reaudit");
                case "skip" -> args.add("--skip-if-exceeding-quota");
                case "test" -> args.add("--test-exceeding-quota");
                case "priority" -> Collections.addAll(args, "--folder-priority-order", "High,Low");
                default -> {}
            }
            AbstractAviatorSSCSastAuditCommand cmd = canonical
                    ? parseCanonical(args.toArray(String[]::new)) : parse(args.toArray(String[]::new));
            var av = new SSCAppVersionDescriptor();
            av.setVersionId("42");

            assertEquals(expectedCount, cmd.getPreflightIssueCount(unirest, av, new AviatorLoggerImpl(new RecordingProgressWriter())));
            assertEquals(expectedRequests, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "true, test, QUOTA_UNKNOWN", "false, test, QUOTA_UNKNOWN",
        "true, skip, SKIPPED", "false, skip, SKIPPED",
        "true, both, SKIPPED", "false, both, SKIPPED"
    })
    void unknownCountReturnsReportWithoutProceedingWithAudit(boolean canonical, String mode, String expectedAction)
            throws Exception {
        var args = new ArrayList<String>();
        args.add("--no-filterset");
        if (!"skip".equals(mode)) {
            args.add("--test-exceeding-quota");
        }
        if (!"test".equals(mode)) {
            args.add("--skip-if-exceeding-quota");
        }
        AbstractAviatorSSCSastAuditCommand cmd = canonical
                ? parseCanonical(args.toArray(String[]::new)) : parse(args.toArray(String[]::new));
        var av = new SSCAppVersionDescriptor();
        av.setVersionId("42");
        av.setApplicationName("TestApp");
        av.setVersionName("1.0");
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/projectVersions/42/issues", exchange -> {
            // An empty page before the reported count is reached makes the total unknown.
            byte[] response = "{\"count\":1,\"data\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try (var unirest = UnirestHelper.createUnirestInstance(instance -> {
            UnirestJsonHeaderConfigurer.configure(instance);
            instance.config().defaultBaseUrl("http://localhost:" + server.getAddress().getPort());
        })) {
            var progress = new RecordingProgressWriter();
            var logger = new AviatorLoggerImpl(progress);
            long count = cmd.getPreflightIssueCount(unirest, av, logger);
            assertEquals(-1L, count);
            // No Aviator URL: quota retrieval fails locally before opening any channel.
            var session = new AviatorUserSessionDescriptor();
            var check = AbstractAviatorSSCSastAuditCommand.class.getDeclaredMethod("checkQuota",
                    UnirestInstance.class, SSCAppVersionDescriptor.class, AviatorUserSessionDescriptor.class,
                    long.class, AviatorLoggerImpl.class);
            check.setAccessible(true);
            var result = (JsonNode) check.invoke(cmd, unirest, av, session, count, logger);
            assertNotNull(result, "An unknown count must return a report instead of allowing an audit");
            assertEquals(expectedAction, result.path(IActionCommandResultSupplier.actionFieldName).asText());
            assertTrue(result.path("operation").path("audit").path("message").asText().contains("Cannot determine issue count"));
            assertTrue(progress.messages.stream().anyMatch(message -> message.contains("issue count unavailable")));
            String actionMessage = "SKIPPED".equals(expectedAction) ? "Audit skipped" : "returning QUOTA_UNKNOWN";
            assertTrue(progress.messages.stream().anyMatch(message -> message.contains(actionMessage)));
            assertFalse(progress.messages.stream().anyMatch(message -> message.contains("Proceeding")
                    || message.contains("Downloading") || message.contains("FPR download")));
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
        "WEBINSPECT; true",
        "SECURITYSCOPE; true",
        "SOMEPLUGIN; true",
        "SCA; false",
        "SCA,WEBINSPECT; false"
    }, delimiter = ';')
    void failIfNoSastResultsRequiresSastScanInArtifacts(String scanTypes, boolean expectedFailure) throws Exception {
        assertSastResultsCheck(expectedFailure, artifact(scanTypes.split(",")));
    }

    @Test
    void failIfNoSastResultsAcceptsSastInAnyArtifact() throws Exception {
        assertSastResultsCheck(false, artifact("WEBINSPECT"), artifact("SCA"));
    }

    @Test
    void failIfNoSastResultsRejectsVersionWithoutScanInformation() throws Exception {
        assertSastResultsCheck(true, JsonHelper.getObjectMapper().createObjectNode());
    }

    @Test
    void failIfNoSastResultsRejectsVersionWithoutArtifacts() throws Exception {
        assertSastResultsCheck(true);
    }

    private static ObjectNode artifact(String... scanTypes) {
        var artifact = JsonHelper.getObjectMapper().createObjectNode();
        var scans = artifact.putObject("_embed").putArray("scans");
        for (String scanType : scanTypes) {
            scans.addObject().put("type", scanType);
        }
        return artifact;
    }

    private static void assertSastResultsCheck(boolean expectedFailure, ObjectNode... artifacts) throws Exception {
        var data = JsonHelper.getObjectMapper().createArrayNode();
        for (var artifact : artifacts) {
            data.add(artifact);
        }
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/projectVersions/42/artifacts", exchange -> {
            byte[] response = JsonHelper.getObjectMapper().createObjectNode().put("count", artifacts.length)
                    .set("data", data).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try (var unirest = UnirestHelper.createUnirestInstance(instance -> {
            UnirestJsonHeaderConfigurer.configure(instance);
            instance.config().defaultBaseUrl("http://localhost:" + server.getAddress().getPort());
        })) {
            var av = new SSCAppVersionDescriptor();
            av.setVersionId("42");
            if (expectedFailure) {
                var exception = assertThrows(AviatorSimpleException.class,
                        () -> AviatorSSCAuditHelper.failIfNoSastResults(unirest, av));
                assertEquals(AviatorSSCAuditHelper.NO_SAST_RESULTS_MESSAGE, exception.getMessage());
            } else {
                AviatorSSCAuditHelper.failIfNoSastResults(unirest, av);
            }
        } finally {
            server.stop(0);
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

    private static AviatorSSCAuditCommand parse(String... args) {
        var cmd = new AviatorSSCAuditCommand();
        parse(cmd, args);
        return cmd;
    }

    private static AviatorSSCSastAuditCommand parseCanonical(String... args) {
        var cmd = new AviatorSSCSastAuditCommand();
        parse(cmd, args);
        return cmd;
    }

    private static void parse(AbstractAviatorSSCSastAuditCommand cmd, String... args) {
        var fullArgs = new ArrayList<String>();
        Collections.addAll(fullArgs, "--av", "test:1.0");
        Collections.addAll(fullArgs, args);
        new CommandLine(cmd).parseArgs(fullArgs.toArray(String[]::new));
    }
}
