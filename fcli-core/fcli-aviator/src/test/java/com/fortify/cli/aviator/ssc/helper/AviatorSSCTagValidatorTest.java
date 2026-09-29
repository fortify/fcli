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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;

/**
 * Pre-upload checks for Aviator prediction, Aviator status, and Analysis values.
 * Assigned tags pass. Built-in Aviator tags returned with {@code includeall=true} pass.
 * A missing tag uses the internal catalog to choose enable-Aviator or prepare guidance.
 */
class AviatorSSCTagValidatorTest {

    private static final String VERSION_ID = "15";
    private static final String ANALYSIS_GUID = "87f2364f-dcd4-49e6-861d-f8d3f351686b";

    private TestSscServer server;
    private UnirestInstance unirest;

    @AfterEach
    void tearDown() {
        if (unirest != null) {
            unirest.close();
        }
        if (server != null) {
            server.close();
        }
    }

    @Test
    @DisplayName("assigned CUSTOM Aviator tags pass without includeall")
    void assignedCustomTagsDoNotWarn() throws IOException {
        server = new TestSscServer()
            .withAssigned(customTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                customTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.isEmpty());
        assertTrue(server.assignedRequests > 0);
        assertTrue(server.includeAllRequests == 0);
    }

    @Test
    @DisplayName("built-in AVIATOR tags returned only by includeall do not warn")
    void builtInAviatorTagsOnIncludeAllDoNotWarn() throws IOException {
        server = new TestSscServer()
            .withAssigned(customTag(ANALYSIS_GUID, "Analysis"))
            .withIncludeAll(
                customTag(ANALYSIS_GUID, "Analysis"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.isEmpty(), warnings.toString());
        assertTrue(server.includeAllRequests > 0);
    }

    @Test
    @DisplayName("CUSTOM tags visible only through includeall still warn")
    void unassignedCustomTagsStillWarn() throws IOException {
        server = new TestSscServer()
            .withAssigned(customTag(ANALYSIS_GUID, "Analysis"))
            .withIncludeAll(
                customTag(ANALYSIS_GUID, "Analysis"),
                customTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                customTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator prediction")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator status")));
        assertTrue(warnings.stream().allMatch(w -> w.contains("fcli aviator ssc prepare")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Enable Aviator")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("customTagType AVIATOR")));
    }

    @Test
    @DisplayName("SSC 26.2+ missing Aviator tags say to enable Aviator")
    void ssc26MissingAviatorTagsSayEnableAviator() throws IOException {
        server = new TestSscServer()
            .withBuiltInAviatorCatalog()
            .withAssigned(customTag(ANALYSIS_GUID, "Analysis"))
            .withIncludeAll(customTag(ANALYSIS_GUID, "Analysis"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator prediction")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator status")));
        assertTrue(warnings.stream().allMatch(w -> w.contains("Enable Aviator in SSC")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("fcli aviator ssc prepare")));
    }

    @Test
    @DisplayName("internal catalog without Aviator tags keeps the prepare warning")
    void internalCatalogWithoutAviatorTagsKeepsPrepare() throws IOException {
        server = new TestSscServer()
            .withInternalCatalog(customTag(ANALYSIS_GUID, "Analysis"))
            .withAssigned(customTag(ANALYSIS_GUID, "Analysis"))
            .withIncludeAll(customTag(ANALYSIS_GUID, "Analysis"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator prediction")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator status")));
        assertTrue(warnings.stream().allMatch(w -> w.contains("fcli aviator ssc prepare")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Enable Aviator")));
    }

    @Test
    @DisplayName("Analysis stays on the assigned list when includeall contains it")
    void analysisTagUsesAssignedList() throws IOException {
        server = new TestSscServer()
            .withAssigned()
            .withIncludeAll(
                customTag(ANALYSIS_GUID, "Analysis"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"));
        unirest = newUnirest(server);

        List<String> warnings = validate(ANALYSIS_GUID, Set.of("Not an Issue"));

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Analysis tag")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Aviator prediction")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Aviator status")));
    }

    @Test
    @DisplayName("includeall failure keeps the assigned-tag warning")
    void includeAllFailureKeepsAssignedWarning() throws IOException {
        server = new TestSscServer()
            .withAssigned(customTag(ANALYSIS_GUID, "Analysis"))
            .failIncludeAll();
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator prediction")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator status")));
        assertTrue(warnings.stream().allMatch(w -> w.contains("fcli aviator ssc prepare")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Enable Aviator")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Pre-upload tag validation failed")));
    }

    private List<String> validate() {
        return validate(null, Set.of());
    }

    private List<String> validate(String analysisTagId, Set<String> analysisTagValues) {
        CollectingLogger logger = new CollectingLogger();
        List<String> warnings = AviatorSSCTagValidator.validatePreUpload(
            unirest, VERSION_ID, analysisTagId, analysisTagValues, logger);
        assertEquals(warnings, logger.warnings);
        return warnings;
    }

    private static UnirestInstance newUnirest(TestSscServer server) {
        return UnirestHelper.createUnirestInstance(unirest -> {
            UnirestJsonHeaderConfigurer.configure(unirest);
            UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
            unirest.config().defaultBaseUrl(server.getBaseUrl());
        });
    }

    private static ObjectNode customTag(String guid, String name) {
        return tag(guid, name, "CUSTOM");
    }

    private static ObjectNode aviatorTag(String guid, String name) {
        return tag(guid, name, "AVIATOR");
    }

    private static ObjectNode tag(String guid, String name, String customTagType) {
        return JsonHelper.getObjectMapper().createObjectNode()
            .put("guid", guid)
            .put("name", name)
            .put("customTagType", customTagType);
    }

    private static final class CollectingLogger implements IAviatorLogger {
        private final List<String> warnings = new ArrayList<>();

        @Override public void progress(String format, Object... args) {}
        @Override public void info(String format, Object... args) {}
        @Override public void warn(String format, Object... args) {
            warnings.add(String.format(format, args));
        }
        @Override public void error(String format, Object... args) {}
    }

    private static final class TestSscServer implements AutoCloseable {
        private final HttpServer server;
        private final ArrayNode assigned = JsonHelper.getObjectMapper().createArrayNode();
        private final ArrayNode includeAll = JsonHelper.getObjectMapper().createArrayNode();
        private final ArrayNode internalCatalog = JsonHelper.getObjectMapper().createArrayNode();
        private boolean failIncludeAll;
        private boolean internalCatalogPresent;
        private int assignedRequests;
        private int includeAllRequests;

        private TestSscServer() throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/projectVersions/" + VERSION_ID + "/customTags", this::handleCustomTags);
            server.createContext("/api/v1/internalCustomTags", this::handleInternalCustomTags);
            server.start();
        }

        private TestSscServer withBuiltInAviatorCatalog() {
            return withInternalCatalog(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"));
        }

        private TestSscServer withInternalCatalog(ObjectNode... tags) {
            this.internalCatalogPresent = true;
            internalCatalog.removeAll();
            for (ObjectNode tag : tags) {
                internalCatalog.add(tag);
            }
            return this;
        }

        private String getBaseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private TestSscServer withAssigned(ObjectNode... tags) {
            assigned.removeAll();
            for (ObjectNode tag : tags) {
                assigned.add(tag);
            }
            return this;
        }

        private TestSscServer withIncludeAll(ObjectNode... tags) {
            includeAll.removeAll();
            for (ObjectNode tag : tags) {
                includeAll.add(tag);
            }
            return this;
        }

        private TestSscServer failIncludeAll() {
            this.failIncludeAll = true;
            return this;
        }

        private void handleInternalCustomTags(HttpExchange exchange) throws IOException {
            if (!internalCatalogPresent) {
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(body);
                }
                return;
            }
            writeJson(exchange, internalCatalog);
        }

        private void handleCustomTags(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getRawQuery();
            boolean includeAllRequest = query != null && query.contains("includeall=true");
            if (includeAllRequest) {
                includeAllRequests++;
                if (failIncludeAll) {
                    byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, body.length);
                    try (OutputStream outputStream = exchange.getResponseBody()) {
                        outputStream.write(body);
                    }
                    return;
                }
                writeJson(exchange, includeAll);
                return;
            }
            assignedRequests++;
            writeJson(exchange, assigned);
        }

        private void writeJson(HttpExchange exchange, ArrayNode data) throws IOException {
            ObjectNode wrapper = JsonHelper.getObjectMapper().createObjectNode();
            wrapper.set("data", data);
            byte[] response = JsonHelper.getObjectMapper()
                .writeValueAsString(wrapper)
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
