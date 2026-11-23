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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.config.AviatorConfigManager;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator.audit.DastAuditFprResult;
import com.fortify.cli.aviator.audit.DastAuditFprStatus;
import com.fortify.cli.aviator.audit.model.AuditFprOptions;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.common.cli.mixin.ICommandAware;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import kong.unirest.UnirestInstance;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
@Isolated("Temporarily overrides stdio and Logback levels")
class AviatorSSCUploadFailureOutputTest {
    private static final String AMBIGUOUS_MESSAGE = "Audit finished, but fcli could not confirm the upload to SSC. "
        + "Check the artifact history before retrying. Quota may have been consumed.";

    @TempDir Path tempDir;
    private final Map<Logger, Level> previousLogLevels = new LinkedHashMap<>();

    @BeforeEach
    void isolateLogging() {
        var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLoggerList().forEach(logger -> {
            previousLogLevels.put(logger, logger.getLevel());
            logger.setLevel(Level.OFF);
        });
    }

    @AfterEach
    void restoreLogging() {
        previousLogLevels.forEach(Logger::setLevel);
    }

    @ParameterizedTest
    @CsvSource({
        "audit, 500", "audit-sast, 500", "audit-dast, 500",
        "audit, 400", "audit-sast, 400", "audit-dast, 400"
    })
    void singleLineHttpFailureStillEmitsCompletedAudit(String commandName, int status) throws Exception {
        var execution = assertDoesNotThrow(() -> execute(commandName, "single-line", status, true),
            "The real single-line writer must not turn a multiline HTTP exception into a progress-writer exception");

        assertEquals(1, execution.exitCode());
        var parsed = JsonHelper.getObjectMapper().readTree(execution.stdout());
        var result = parsed.isArray() ? parsed.get(0) : parsed;
        assertAll(
            () -> assertEquals("42", result.path("id").asText()),
            () -> assertEquals("TestApp", result.path("applicationName").asText()),
            () -> assertEquals("1.0", result.path("versionName").asText()),
            () -> assertEquals("AUDITED_NOT_UPLOADED", result.path("__action__").asText()),
            () -> assertTrue(result.path("artifactId").isNull()),
            () -> assertEquals(3, result.path("operation").path("audit").path("submitted").asInt()),
            () -> assertEquals(2, result.path("operation").path("audit").path("succeeded").asInt()),
            () -> assertEquals(1, result.path("operation").path("audit").path("skipped").asInt()));
        assertOutcomeMessage(result.path("operation").path("audit").path("message").asText(), status);
        assertEquals(1, execution.uploads(), "An uncertain result must not trigger an automatic upload retry");
    }

    @ParameterizedTest
    @CsvSource({
        "audit, 500", "audit-sast, 500", "audit-dast, 500",
        "audit, 400", "audit-sast, 400", "audit-dast, 400",
        "audit, 200", "audit-sast, 200", "audit-dast, 200"
    })
    void progressNoneDefaultTableShowsFinalUploadOutcome(String commandName, int status) throws Exception {
        var execution = execute(commandName, "none", status, false);

        assertEquals(1, execution.exitCode());
        String table = execution.stdout().replaceAll("\\s+", " ");
        assertTrue(table.contains("AUDITED_NOT_UPLOADED"), table);
        assertTrue(execution.stdout().lines().anyMatch(line -> line.stripLeading().startsWith("42 ")), table);
        assertOutcomeMessage(table, status);
        assertTrue(table.contains("partial audit detail"), "Keep the completed audit message alongside the upload outcome: " + table);
        assertEquals("", execution.progress(), "The final table message must not depend on progress output");
        assertEquals(1, execution.uploads());
    }

    private static void assertOutcomeMessage(String message, int status) {
        if (status == 400) {
            assertTrue(message.contains("SSC rejected the upload request"), message);
            assertFalse(message.contains("could not confirm the upload"), message);
            assertTrue(message.toLowerCase().contains("quota may have been consumed"), message);
            assertTrue(message.contains("Retrying the audit may consume more quota"), message);
        } else {
            assertTrue(message.contains(AMBIGUOUS_MESSAGE), message);
            assertFalse(message.toLowerCase().contains("upload failed"), message);
            assertFalse(message.toLowerCase().contains("ssc rejected"), message);
        }
        assertTrue(message.toLowerCase().contains("retry"), message);
        if (status == 200) {
            assertTrue(message.contains("artifact ID"), message);
        } else {
            assertAll(
                () -> assertTrue(message.contains("HTTP " + status), message),
                () -> assertTrue(message.contains("Request:"), message),
                () -> assertTrue(message.contains("/api/v1/projectVersions/42/artifacts"), message),
                () -> assertTrue(message.contains("Body:"), message),
                () -> assertTrue(message.contains("ssc-upload-diagnostic"), message));
        }
    }

    private Execution execute(String commandName, String progressType, int status, boolean json) throws Exception {
        Path fpr = createFpr();
        var uploads = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/projectVersions/42/customTags", exchange -> respond(exchange, 200, "{}"));
        server.createContext("/api/v1/projectVersions/42/artifacts", exchange -> {
            exchange.getRequestBody().readAllBytes();
            uploads.incrementAndGet();
            respond(exchange, status, status == 200 ? "{\"data\":{}}" : "{\"message\":\"ssc-upload-diagnostic\"}");
        });
        server.start();
        try (var unirest = UnirestHelper.createUnirestInstance(instance -> {
            UnirestJsonHeaderConfigurer.configure(instance);
            UnirestUnexpectedHttpResponseConfigurer.configure(instance);
            instance.config().defaultBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        })) {
            AbstractOutputCommand command = createCommand(commandName, unirest, fpr);
            configure(command, progressType, json);
            return capture(command, uploads);
        } finally {
            server.stop(0);
        }
    }

    private AbstractOutputCommand createCommand(String name, UnirestInstance unirest, Path fpr) {
        var av = new SSCAppVersionDescriptor();
        av.setVersionId("42");
        av.setApplicationName("TestApp");
        av.setVersionName("1.0");
        var sastResult = new FPRAuditResult(fpr.toFile(), "PARTIALLY_AUDITED", "partial audit detail",
            2, 3, 3, 1, Map.of("Skipped by Aviator", 1), 0, Map.of());
        return switch (name) {
            case "audit-sast" -> new AviatorSSCSastAuditCommand() {
                @Override public UnirestInstance getUnirestInstance() { return unirest; }
                @Override public JsonNode getJsonNode(UnirestInstance instance) {
                    try (var progress = progressWriter(getCommandHelper().getCommandSpec())) {
                        return processFpr(instance, av, "unused", "unused", new AviatorLoggerImpl(progress), fpr);
                    }
                }
                @Override FPRAuditResult auditFpr(AuditFprOptions options) { return sastResult; }
            };
            case "audit" -> new AviatorSSCAuditCommand() {
                @Override public UnirestInstance getUnirestInstance() { return unirest; }
                @Override public JsonNode getJsonNode(UnirestInstance instance) {
                    try (var progress = progressWriter(getCommandHelper().getCommandSpec())) {
                        return processFpr(instance, av, "unused", "unused", new AviatorLoggerImpl(progress), fpr);
                    }
                }
                @Override FPRAuditResult auditFpr(AuditFprOptions options) { return sastResult; }
            };
            case "audit-dast" -> new AviatorSSCDastAuditCommand() {
                @Override public UnirestInstance getUnirestInstance() { return unirest; }
                @Override public JsonNode getJsonNode(UnirestInstance instance) {
                    try (var progress = progressWriter(getCommandHelper().getCommandSpec())) {
                        var mapping = AviatorConfigManager.getInstance().getDefaultDastTagMappingConfig().resolveForDast();
                        return processFpr(instance, av, fpr, new AviatorUserSessionDescriptor(),
                            new AviatorLoggerImpl(progress), mapping, progress);
                    } catch (Exception e) {
                        throw new AssertionError("Completed DAST audit must survive an upload error", e);
                    }
                }
                @Override DastAuditFprResult auditFpr(Path path, SSCAppVersionDescriptor appVersion,
                        AviatorUserSessionDescriptor session, IAviatorLogger logger, TagMappingConfig mapping) {
                    return DastAuditFprResult.builder().status(DastAuditFprStatus.PARTIALLY_AUDITED)
                        .message("partial audit detail").updatedFile(fpr.toFile())
                        .totalReported(3).eligible(3).submitted(3).succeeded(2).skipped(1).build();
                }
            };
            default -> throw new AssertionError("Unexpected command: " + name);
        };
    }

    private static void configure(AbstractOutputCommand command, String progressType, boolean json) {
        var root = new CommandLine(CommandSpec.create().name("fcli"));
        var aviator = new CommandLine(CommandSpec.create().name("aviator"));
        var ssc = new CommandLine(CommandSpec.create().name("ssc"));
        var leaf = new CommandLine(command);
        root.addSubcommand("aviator", aviator);
        aviator.addSubcommand("ssc", ssc);
        ssc.addSubcommand(leaf.getCommandName(), leaf);
        leaf.setResourceBundle(ResourceBundle.getBundle("com.fortify.cli.aviator.i18n.AviatorMessages"));
        // Use the shipped table columns, not a custom projection that already includes the missing message.
        root.parseArgs("aviator", "ssc", leaf.getCommandName(),
            "--av", "TestApp:1.0", "--progress", progressType, "-o", json ? "json" : "table");
        FcliCommandSpecHelper.getAllUserObjectsStream(leaf.getCommandSpec()).forEach(object -> {
            if (object instanceof ICommandAware commandAware) {
                commandAware.setCommandSpec(leaf.getCommandSpec());
            }
        });
    }

    private static IProgressWriter progressWriter(CommandSpec spec) {
        return FcliCommandSpecHelper.getAllUserObjectsStream(spec)
            .filter(ProgressWriterFactoryMixin.class::isInstance)
            .map(ProgressWriterFactoryMixin.class::cast).findFirst().orElseThrow().create();
    }

    private static Execution capture(AbstractOutputCommand command, AtomicInteger uploads) {
        var stdout = new ByteArrayOutputStream();
        var stderr = new ByteArrayOutputStream();
        var progress = new ByteArrayOutputStream();
        var previousOut = System.out;
        var previousErr = System.err;
        var previousProgressOut = StdioHelper.getProgressOut();
        var previousProgressErr = StdioHelper.getProgressErr();
        try (var out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
             var err = new PrintStream(stderr, true, StandardCharsets.UTF_8);
             var progressOut = new PrintStream(progress, true, StandardCharsets.UTF_8)) {
            System.setOut(out);
            System.setErr(err);
            StdioHelper.pushOut(out);
            StdioHelper.pushErr(err);
            StdioHelper.setProgressOut(progressOut);
            StdioHelper.setProgressErr(progressOut);
            try {
                int exitCode = command.call();
                return new Execution(exitCode, stdout.toString(StandardCharsets.UTF_8),
                    progress.toString(StandardCharsets.UTF_8), uploads.get());
            } finally {
                StdioHelper.setProgressOut(previousProgressOut);
                StdioHelper.setProgressErr(previousProgressErr);
                StdioHelper.popOut();
                StdioHelper.popErr();
                System.setOut(previousOut);
                System.setErr(previousErr);
            }
        }
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

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private record Execution(int exitCode, String stdout, String progress, int uploads) {}
}
