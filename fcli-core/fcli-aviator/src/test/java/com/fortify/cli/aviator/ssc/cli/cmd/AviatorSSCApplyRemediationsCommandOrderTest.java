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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.remediations_cache.IApplyRemediationsOptions;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.fpr.utils.SourceDecoders;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsSelectorArgGroups.OnlineModeArgGroup;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsSelectorArgGroups.OnlineSelectionArgGroup;
import com.fortify.cli.aviator.ssc.helper.SSCOnlineRemediationsFprSource;
import com.fortify.cli.aviator.util.FileUtil;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.ProgressWriterType;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * The order in which {@code apply-remediations --all} walks an application version's artifacts is a
 * yield decision, and the current order is the losing one.
 *
 * <p>{@code SSCArtifactHelper.getAllAviatorArtifacts} fetches {@code uploadDate DESC} and then
 * reverses the list "to maintain ascending order contract", so
 * {@code AviatorSSCApplyRemediationsCommand} applies the OLDEST scan first. The oldest scan is the
 * one whose line numbers are the most stale relative to the customer's current checkout: letting it
 * go first rewrites the file and destroys the newest scan's chance of a clean hash match, after
 * which the newest scan has to fall back on fuzzy anchoring and can lose its hunk to an ambiguous
 * match. Newest-first applies the best-matching artifact while the file is still pristine, and the
 * older artifacts then simply fail their own anchor checks, which is the correct outcome.
 *
 * <p>This is NOT a correctness test. Whatever gets written is written in the right place in either
 * order - that is guaranteed by anchor verification, not by ordering. What the order costs us is
 * fixes we could have landed. {@code getAllAviatorArtifacts} has exactly one caller, this command,
 * so the fix can be a local reverse in the {@code --all} loop or a change to the helper's ordering
 * contract; this test only cares that the newest artifact is applied first.
 *
 * <p>Currently: red, {@code appliedRemediation} is 1. Turns green when the order is reversed.
 */
class AviatorSSCApplyRemediationsCommandOrderTest {
    private static final String APP_VERSION_ID = "42";

    /**
     * Two identical {@code ctx/DUP/ctx2} blocks, so the newer artifact's hunk can only be placed by
     * its declared line number - fuzzy search on its own finds two candidates and refuses to guess.
     */
    private static final String PRISTINE_SOURCE =
        "head\nOLDTARGET\nctx\nDUP\nctx2\nfiller\nctx\nDUP\nctx2\ntail\n";

    @TempDir
    Path tempDir;

    @Test
    void allOpenIssuesAppliesNewestArtifactFirstSoFewerRemediationsAreLost() throws Exception {
        Path sourceFile = tempDir.resolve("Example.java");
        Files.writeString(sourceFile, PRISTINE_SOURCE, StandardCharsets.UTF_8);

        // Artifact 1, the older scan: stale declared line, a hash that cannot match, and a NewCode
        // one line longer than what it replaces, so applying it shifts everything below it.
        byte[] olderFpr = buildFpr("old-fix", "not-the-source-hash",
            4, 4, "head\nOLDTARGET\nctx", "OLDTARGET", "OLD1\nOLD2");
        // Artifact 2, the newer scan: correct declared line and a hash that matches the pristine file.
        byte[] newerFpr = buildFpr("new-fix", canonicalHashOf(PRISTINE_SOURCE),
            8, 8, "ctx\nDUP\nctx2", "DUP", "NEWFIX");

        try (TestSscServer server = new TestSscServer(Map.of("1", olderFpr, "2", newerFpr));
                UnirestInstance unirest = newUnirest(server)) {
            var resolved = allArtifactsSelector().resolveArtifacts(unirest, null);
            assertEquals(List.of("2", "1"), resolved.artifacts().stream().map(artifact -> artifact.getId()).toList());

            var progressWriterFactory = silentProgressWriterFactory();
            try (var progressWriter = progressWriterFactory.create();
                    var source = new SSCOnlineRemediationsFprSource(
                        unirest, new AviatorLoggerImpl(progressWriter), progressWriter, resolved)) {
                var applyResult = RemediationsApplyHelper.apply(
                    source, applyOptions(), null, new AviatorLoggerImpl(progressWriter));
                var result = AviatorRemediationMetricsHelper.aggregateMetrics(null, applyResult.metrics());

                assertEquals(2, applyResult.metrics().size());
                assertEquals(2, result.appliedRemediations(),
                    "newest-first lands both fixes; oldest-first loses the newer artifact's hunk to an "
                        + "ambiguous fuzzy match after the older artifact has shifted the file");
                assertEquals("head\nOLD1\nOLD2\nctx\nDUP\nctx2\nfiller\nctx\nNEWFIX\nctx2\ntail\n",
                    Files.readString(sourceFile));
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Command wiring - same reflective approach as FoDAviatorApplyRemediationsCommandTest, since
    // the command's fields are picocli mixins that are otherwise only populated by a real parse.
    // ------------------------------------------------------------------------------------------

    private static OnlineSelectionArgGroup allArtifactsSelector() throws Exception {
        var selector = new OnlineSelectionArgGroup();
        var mode = new OnlineModeArgGroup();
        set(mode, "all", true);
        set(selector, "mode", mode);
        set(selector, "appVersionNameOrId", APP_VERSION_ID);
        set(selector, "delimiter", ":");
        return selector;
    }

    private IApplyRemediationsOptions applyOptions() {
        return new IApplyRemediationsOptions() {
            @Override public String getSourceCodeDirectory() { return tempDir.toString(); }
            @Override public List<String> getIssueIds() { return List.of(); }
            @Override public ISourceDecoder getSourceDecoder() { return SourceDecoders.defaults(); }
            @Override public boolean isPreviewMode() { return false; }
            @Override public void validate() {}
        };
    }

    private static ProgressWriterFactoryMixin silentProgressWriterFactory() throws Exception {
        var factory = new ProgressWriterFactoryMixin();
        set(factory, "type", ProgressWriterType.none);
        var commandHelper = new CommandHelperMixin();
        // setCommandSpec walks up to the root CommandLine, so the spec must be attached to one.
        commandHelper.setCommandSpec(new CommandLine(CommandSpec.create()).getCommandSpec());
        set(factory, "commandHelper", commandHelper);
        return factory;
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " on " + target.getClass());
    }

    private static UnirestInstance newUnirest(TestSscServer server) {
        return UnirestHelper.createUnirestInstance(unirest -> {
            UnirestJsonHeaderConfigurer.configure(unirest);
            unirest.config().defaultBaseUrl(server.getBaseUrl());
        });
    }

    // ------------------------------------------------------------------------------------------
    // Minimal SSC stub: application version lookup, artifact listing (uploadDate DESC, as SSC
    // returns it), file tokens, and artifact download.
    // ------------------------------------------------------------------------------------------

    private static final class TestSscServer implements AutoCloseable {
        private final HttpServer server;
        /** Artifact id -&gt; FPR bytes, in the order SSC returns them: newest uploadDate first. */
        private final Map<String, byte[]> fprsByArtifactIdNewestFirst = new LinkedHashMap<>();

        private TestSscServer(Map<String, byte[]> fprsByArtifactId) throws IOException {
            fprsByArtifactId.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .forEach(e -> fprsByArtifactIdNewestFirst.put(e.getKey(), e.getValue()));
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/projectVersions", this::handleProjectVersions);
            server.createContext("/api/v1/fileTokens", this::handleFileTokens);
            server.createContext("/download/artifactDownload.html", this::handleDownload);
            server.start();
        }

        private String getBaseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        private void handleProjectVersions(HttpExchange exchange) throws IOException {
            if (exchange.getRequestURI().getPath().endsWith("/artifacts")) {
                handleArtifacts(exchange);
            } else {
                ObjectNode body = JsonHelper.getObjectMapper().createObjectNode();
                body.putArray("data").addObject().put("id", APP_VERSION_ID);
                respondJson(exchange, body);
            }
        }

        /** SSC returns uploadDate DESC, i.e. newest artifact first; the helper then reverses it. */
        private void handleArtifacts(HttpExchange exchange) throws IOException {
            ArrayNode data = JsonHelper.getObjectMapper().createArrayNode();
            int month = 9;
            for (String artifactId : fprsByArtifactIdNewestFirst.keySet()) {
                data.addObject()
                    .put("id", artifactId)
                    .put("originalFileName", "aviator_" + artifactId + ".fpr")
                    .put("uploadDate", "2026-0" + month-- + "-01T00:00:00.000+0000")
                    .put("projectVersionId", APP_VERSION_ID);
            }
            ObjectNode body = JsonHelper.getObjectMapper().createObjectNode();
            body.put("count", data.size());
            body.set("data", data);
            respondJson(exchange, body);
        }

        private void handleFileTokens(HttpExchange exchange) throws IOException {
            ObjectNode body = JsonHelper.getObjectMapper().createObjectNode();
            body.putObject("data").put("token", "test-download-token");
            respondJson(exchange, body);
        }

        private void handleDownload(HttpExchange exchange) throws IOException {
            byte[] fpr = fprsByArtifactIdNewestFirst.get(
                queryParam(exchange.getRequestURI().getRawQuery(), "id"));
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(fpr == null ? 404 : 200, fpr == null ? -1 : fpr.length);
            if (fpr != null) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(fpr);
                }
            }
        }

        private static String queryParam(String rawQuery, String name) {
            for (String pair : rawQuery == null ? new String[0] : rawQuery.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).equals(name)) {
                    return pair.substring(eq + 1);
                }
            }
            return null;
        }

        private static void respondJson(HttpExchange exchange, ObjectNode body) throws IOException {
            byte[] response = body.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    // ------------------------------------------------------------------------------------------
    // FPR construction
    // ------------------------------------------------------------------------------------------

    /** The Hash an FPR declares for a file it really was scanned against. */
    private static String canonicalHashOf(String content) throws Exception {
        byte[] bytes = FileUtil.canonicalizeForHash(content).getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static byte[] buildFpr(String instanceId, String hash, int lineFrom, int lineTo,
            String context, String originalCode, String newCode) throws IOException {
        String remediationXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <r:Remediations xmlns:r="xmlns://www.fortify.com/schema/remediations">
                  <r:ProjectInfo>
                    <r:Name>test</r:Name>
                    <r:WriteDate>2026-08-26T00:00:00Z</r:WriteDate>
                  </r:ProjectInfo>
                  <r:RemediationList>
                    <r:Remediation instanceId="%s">
                      <r:AuditComment>test</r:AuditComment>
                      <r:FileChanges>
                        <r:Filename>Example.java</r:Filename>
                        <r:Hash type="SHA-256">%s</r:Hash>
                        <r:Change>
                          <r:LineFrom>%d</r:LineFrom>
                          <r:LineTo>%d</r:LineTo>
                          <r:Context before="1" after="1">%s</r:Context>
                          <r:OriginalCode>%s</r:OriginalCode>
                          <r:NewCode>%s</r:NewCode>
                        </r:Change>
                      </r:FileChanges>
                    </r:Remediation>
                  </r:RemediationList>
                </r:Remediations>
                """.formatted(instanceId, hash, lineFrom, lineTo, context, originalCode, newCode);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("remediations.xml"));
            zip.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
