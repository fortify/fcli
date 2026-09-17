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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;

class AviatorSSCAttributeHelperTest {
    @Test
    void writesLastDastAuditTimestampAsTextAttribute() throws Exception {
        try (var server = new TestSscServer(); var unirest = newUnirest(server)) {
            AviatorSSCAttributeHelper.writeLastDastAuditTimestamp(unirest, "42");

            JsonNode update = server.getLastUpdate();
            assertEquals("42", update.get(0).path("attributeDefinitionId").asText());
            assertTrue(update.get(0).path("value").asText().matches("\\d{4}-\\d{2}-\\d{2}T.*Z"));
        }
    }

    @Test
    void exposesStableDastAuditAttributeDefinition() {
        var definition = AviatorSSCAttributeDefinitions.LAST_DAST_AUDIT_ATTR;

        assertEquals("last_dast_audit", definition.name());
        assertEquals("TECHNICAL", definition.category());
        assertEquals("TEXT", definition.type());
    }

    @Test
    void markerWriteFailureIsReportedWithoutThrowing() throws Exception {
        try (var server = new TestSscServer().withUpdateStatus(500); var unirest = newUnirest(server)) {
            assertDoesNotThrow(() -> AviatorSSCAttributeHelper.writeLastDastAuditTimestamp(unirest, "42"));
        }
    }

    private static UnirestInstance newUnirest(TestSscServer server) {
        return UnirestHelper.createUnirestInstance(unirest -> {
            UnirestJsonHeaderConfigurer.configure(unirest);
            unirest.config().defaultBaseUrl(server.getBaseUrl());
        });
    }

    private static final class TestSscServer implements AutoCloseable {
        private final HttpServer server;
        private JsonNode lastUpdate;
        private int updateStatus = 200;

        private TestSscServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/attributeDefinitions", this::handleDefinitions);
            server.createContext("/api/v1/projectVersions/42/attributes", this::handleAttributes);
            server.start();
        }

        private String getBaseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        private JsonNode getLastUpdate() {
            return lastUpdate;
        }

        private TestSscServer withUpdateStatus(int status) {
            updateStatus = status;
            return this;
        }

        private void handleDefinitions(HttpExchange exchange) throws IOException {
            respond(exchange, 200, """
                {"data":[{"id":"42","guid":"C3D4E5F6-A7B8-9012-BCDE-F12345678902","name":"last_dast_audit","category":"TECHNICAL","type":"TEXT","required":false,"hasDefault":false,"options":[]}]}
                """);
        }

        private void handleAttributes(HttpExchange exchange) throws IOException {
            if (!"PUT".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "{}");
                return;
            }
            lastUpdate = JsonHelper.getObjectMapper().readTree(exchange.getRequestBody());
            respond(exchange, updateStatus, "{}");
        }

        private static void respond(HttpExchange exchange, int status, String body) throws IOException {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}