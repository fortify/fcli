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

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAuditHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;
import picocli.CommandLine;

class AviatorSSCAuditCommandTest {
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
