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
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;

class AviatorSSCAuditHelperTest {

    @Test
    void excludesPreviouslyProcessedIssuesFromNormalPreflightCount() throws Exception {
        try (var server = new TestSscServer(processedIssue(), unprocessedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, false));
            assertTrue(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forceReauditPreflightIncludesPreviouslyProcessedIssues() throws Exception {
        try (var server = new TestSscServer(processedIssue(), unprocessedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(2, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertFalse(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forceReauditPreflightDoesNotSkipAnAllProcessedApplicationVersion() throws Exception {
        try (var server = new TestSscServer(processedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertFalse(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forceReauditPreflightExcludesHumanAuditedIssuesThatAviatorNeverProcessed() throws Exception {
        try (var server = new TestSscServer(processedIssue(), unprocessedIssue(), humanAuditedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(2, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
            assertFalse(server.getLastIssueQuery().contains("q=audited:false"));
        }
    }

    @Test
    void forceReauditPreflightExcludesSuppressedAviatorIssues() throws Exception {
        try (var server = new TestSscServer(processedIssue(), suppressedProcessedIssue());
                var unirest = newUnirest(server)) {
            assertEquals(1, AviatorSSCAuditHelper.getAuditableIssueCount(
                    unirest, appVersion(), logger(), true, null, null, true));
        }
    }

    @Test
    void forceReauditCategoryBreakdownIncludesFullyAuditedCategories() throws Exception {
        try (var server = new TestSscServer(processedIssue());
                var unirest = newUnirest(server)) {
            List<Map<String, Object>> categories = AviatorSSCAuditHelper.getTopUnauditedCategories(
                    unirest, appVersion(), logger(), 10, true);

            assertEquals(1, categories.size());
            assertEquals("Category A", categories.get(0).get("categoryName"));
            assertEquals(1L, categories.get(0).get("unauditedCount"));
            assertEquals(1, server.getSelectorSetRequestCount());
        }
    }

    private static SSCAppVersionDescriptor appVersion() {
        var descriptor = new SSCAppVersionDescriptor();
        descriptor.setVersionId("42");
        descriptor.setApplicationName("TestApp");
        descriptor.setVersionName("1.0");
        return descriptor;
    }

    private static AviatorLoggerImpl logger() {
        return new AviatorLoggerImpl(new NoOpProgressWriter());
    }

    private static UnirestInstance newUnirest(TestSscServer server) {
        return UnirestHelper.createUnirestInstance(unirest -> {
            UnirestJsonHeaderConfigurer.configure(unirest);
            unirest.config().defaultBaseUrl(server.getBaseUrl());
        });
    }

    private static ObjectNode processedIssue() {
        ObjectNode issue = JsonHelper.getObjectMapper().createObjectNode();
        issue.put("audited", true);
        ArrayNode auditValues = issue.putObject("_embed").putArray("auditValues");
        auditValues.addObject()
                .put("customTagGuid", AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid())
                .put("customTagIndex", 0);
        return issue;
    }

    private static ObjectNode unprocessedIssue() {
        ObjectNode issue = JsonHelper.getObjectMapper().createObjectNode();
        issue.put("audited", false);
        issue.putObject("_embed").putArray("auditValues");
        return issue;
    }

    private static ObjectNode humanAuditedIssue() {
        ObjectNode issue = JsonHelper.getObjectMapper().createObjectNode();
        issue.put("audited", true);
        issue.putObject("_embed").putArray("auditValues");
        return issue;
    }

    private static ObjectNode suppressedProcessedIssue() {
        ObjectNode issue = processedIssue();
        issue.put("suppressed", true);
        return issue;
    }

    private static final class TestSscServer implements AutoCloseable {
        private final HttpServer server;
        private final ArrayNode issues = JsonHelper.getObjectMapper().createArrayNode();
        private int selectorSetRequestCount;
        private String lastIssueQuery = "";

        private TestSscServer(ObjectNode... issueNodes) throws IOException {
            for (ObjectNode issue : issueNodes) {
                issues.add(issue);
            }
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/projectVersions/42/issues", this::handleIssues);
            server.createContext("/api/v1/projectVersions/42/issueSelectorSet", this::handleSelectorSet);
            server.createContext("/api/v1/projectVersions/42/issueGroups", this::handleIssueGroups);
            server.start();
        }

        private String getBaseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        private void handleIssues(HttpExchange exchange) throws IOException {
            lastIssueQuery = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            ArrayNode matchingIssues = JsonHelper.getObjectMapper().createArrayNode();
            boolean unauditedOnly = lastIssueQuery.contains("q=audited:false");
            for (JsonNode issue : issues) {
                if (unauditedOnly && issue.path("audited").asBoolean(false)) {
                    continue;
                }
                matchingIssues.add(issue);
            }
            byte[] response = JsonHelper.getObjectMapper().createObjectNode()
                    .put("count", matchingIssues.size())
                    .set("data", matchingIssues)
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        }

        private void handleSelectorSet(HttpExchange exchange) throws IOException {
            selectorSetRequestCount++;
            ObjectNode data = JsonHelper.getObjectMapper().createObjectNode();
            data.putArray("filterBySet");
            data.putArray("groupBySet").addObject()
                    .put("guid", "category-guid")
                    .put("displayName", "Category");
            writeJson(exchange, JsonHelper.getObjectMapper().createObjectNode().set("data", data));
        }

        private void handleIssueGroups(HttpExchange exchange) throws IOException {
            ObjectNode group = JsonHelper.getObjectMapper().createObjectNode()
                    .put("id", "Category A")
                    .put("visibleCount", 1)
                    .put("auditedCount", 1);
            ArrayNode data = JsonHelper.getObjectMapper().createArrayNode().add(group);
            writeJson(exchange, JsonHelper.getObjectMapper().createObjectNode().set("data", data));
        }

        private void writeJson(HttpExchange exchange, ObjectNode responseNode) throws IOException {
            byte[] response = responseNode.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        }

        private String getLastIssueQuery() {
            return lastIssueQuery;
        }

        private int getSelectorSetRequestCount() {
            return selectorSetRequestCount;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class NoOpProgressWriter implements IProgressWriter {
        @Override public boolean isMultiLineSupported() { return false; }
        @Override public void writeProgress(String message, Object... args) {}
        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) {}
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void clearProgress() {}
        @Override public void close() {}
        @Override public String type() { return "test"; }
    }
}
