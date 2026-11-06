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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCustomTagHelper.SynchronizationResult;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCTagDefs.TagDefinition;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;

/**
 * Unit tests for {@link AviatorSSCCustomTagHelper} capability detection
 * for SSC 26.2+ system-managed Aviator tags.
 *
 * <p>These tests focus on the {@link SynchronizationResult} model class behavior
 * since the actual HTTP calls require a live SSC instance or mock framework.
 */
class AviatorSSCCustomTagHelperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("SynchronizationResult")
    class SynchronizationResultTests {

        @Test
        @DisplayName("success with non-system-managed tag should require association")
        void successNonSystemManaged_requiresAssociation() {
            JsonNode tag = MAPPER.createObjectNode().put("id", "123").put("name", "Aviator prediction");
            SynchronizationResult result = SynchronizationResult.success(tag, false);

            assertTrue(result.isSuccessful(), "Should be successful");
            assertFalse(result.isSystemManaged(), "Should NOT be system-managed");
            assertTrue(result.requiresAssociation(), "Non-system-managed tag should require association");
            assertNotNull(result.getTag(), "Tag should not be null");
            assertEquals("123", result.getTag().get("id").asText());
        }

        @Test
        @DisplayName("success with system-managed tag should NOT require association")
        void successSystemManaged_doesNotRequireAssociation() {
            JsonNode tag = MAPPER.createObjectNode().put("id", "456").put("name", "Aviator status");
            SynchronizationResult result = SynchronizationResult.success(tag, true);

            assertTrue(result.isSuccessful(), "Should be successful");
            assertTrue(result.isSystemManaged(), "Should be system-managed");
            assertFalse(result.requiresAssociation(), "System-managed tag should NOT require association");
            assertNotNull(result.getTag(), "Tag should not be null");
        }

        @Test
        @DisplayName("failure should not require association and have null tag")
        void failure_doesNotRequireAssociation() {
            SynchronizationResult result = SynchronizationResult.failure();

            assertFalse(result.isSuccessful(), "Should NOT be successful");
            assertFalse(result.isSystemManaged(), "Failure should not be system-managed");
            assertFalse(result.requiresAssociation(), "Failed sync should NOT require association");
            assertNull(result.getTag(), "Failed sync should have null tag");
        }

        @Test
        @DisplayName("requiresAssociation is false when not successful even if not system-managed")
        void notSuccessful_doesNotRequireAssociation() {
            // Edge case: construct manually with successful=false but systemManaged=false
            SynchronizationResult result = new SynchronizationResult(null, false, false);

            assertFalse(result.requiresAssociation(),
                    "Should NOT require association when not successful, regardless of systemManaged flag");
        }
    }

    @Nested
    @DisplayName("PrepareResult entry tracking")
    class PrepareResultTests {

        @Test
        @DisplayName("PrepareResult correctly tracks entries")
        void prepareResult_tracksEntries() {
            var result = new AviatorSSCPrepareHelper.PrepareResult();

            result.addEntry("Custom Tag", "VERIFIED", "Tag 'Aviator prediction' is already present.");
            result.addEntry("Custom Tag", "SYSTEM_MANAGED", "'Aviator status' is a built-in SSC tag.");
            result.addEntry("Global", "INFO", "All Aviator tags are system-managed.");

            assertEquals(3, result.getEntries().size());

            JsonNode json = result.toJsonNode();
            assertTrue(json.isArray());
            assertEquals(3, json.size());
            assertEquals("VERIFIED", json.get(0).get("status").asText());
            assertEquals("SYSTEM_MANAGED", json.get(1).get("status").asText());
            assertEquals("INFO", json.get(2).get("status").asText());
        }

        @Test
        @DisplayName("PrepareResult toJsonNode includes all fields")
        void prepareResult_toJsonNode_includesAllFields() {
            var result = new AviatorSSCPrepareHelper.PrepareResult();
            result.addEntry("Custom Tag", "CREATED", "Tag 'DAST correlation status' created.");

            JsonNode json = result.toJsonNode();
            JsonNode entry = json.get(0);

            assertEquals("CREATED", entry.get("status").asText());
            assertEquals("Custom Tag", entry.get("entity").asText());
            assertEquals("Tag 'DAST correlation status' created.", entry.get("details").asText());
        }
    }

    @Nested
    @DisplayName("TagDefinition behavior")
    class TagDefinitionTests {

        @Test
        @DisplayName("AVIATOR_PREDICTION_TAG has correct GUID and values")
        void aviatorPredictionTag_hasCorrectGuidAndValues() {
            var tag = AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG;

            assertEquals("C2D6EC66-CCB3-4FB9-9EE0-0BB02F51008F", tag.getGuid());
            assertEquals("Aviator prediction", tag.getName());
            assertEquals("LIST", tag.getValueType());
            assertEquals(6, tag.getValues().size());
            assertTrue(tag.getValues().contains("AVIATOR:Not an Issue"));
            assertTrue(tag.getValues().contains("AVIATOR:Remediation Required"));
        }

        @Test
        @DisplayName("AVIATOR_STATUS_TAG has correct GUID and values")
        void aviatorStatusTag_hasCorrectGuidAndValues() {
            var tag = AviatorSSCTagDefs.AVIATOR_STATUS_TAG;

            assertEquals("FB7B0462-2C2E-46D9-811A-DCC1F3C83051", tag.getGuid());
            assertEquals("Aviator status", tag.getName());
            assertEquals("LIST", tag.getValueType());
            assertEquals(2, tag.getValues().size());
            assertTrue(tag.getValues().contains("PROCESSED_BY_AVIATOR"));
            assertTrue(tag.getValues().contains("PROCESSED_BY_AVIATOR_WITH_REMEDIATION"));
        }

        @Test
        @DisplayName("DAST_CORRELATION_STATUS_TAG is a TEXT type with no predefined values")
        void dastCorrelationTag_isTextType() {
            var tag = AviatorSSCTagDefs.DAST_CORRELATION_STATUS_TAG;

            assertEquals("7A3B5C9D-1E2F-4A8B-9C0D-E1F2A3B4C5D6", tag.getGuid());
            assertEquals("DAST correlation status", tag.getName());
            assertEquals("TEXT", tag.getValueType());
            assertTrue(tag.getValues().isEmpty(), "TEXT tags should have empty value list");
        }
    }

    @Nested
    @DisplayName("Integration tests with mock SSC server")
    class IntegrationTests {

        @Test
        void testSynchronizeUsesExistingCustomTag() throws Exception {
            try (var server = new TestSscServer()) {
                String tagId = "1001";
                server.withCustomTags(tagSummary(tagId, AviatorSSCTagDefs.AVIATOR_STATUS_TAG));
                server.withCustomTagDetails(tagId, tagDetails(tagId, AviatorSSCTagDefs.AVIATOR_STATUS_TAG));

                try (var unirest = newUnirest(server)) {
                    var result = new AviatorSSCPrepareHelper.PrepareResult();
                    SynchronizationResult syncResult = new AviatorSSCCustomTagHelper(unirest, AviatorSSCTagDefs.AVIATOR_STATUS_TAG)
                        .synchronize(result);

                    assertNotNull(syncResult);
                    assertNotNull(syncResult.getTag());
                    assertEquals(tagId, syncResult.getTag().get("id").asText());
                    assertEquals(2, syncResult.getTag().withArray("valueList").size());
                    assertEquals(1, server.getCustomTagsGetCount());
                    assertEquals(1, server.getCustomTagDetailsGetCount());
                    assertEquals(0, server.getCustomTagCreateCount());
                    assertEquals(0, server.getCustomTagUpdateCount());
                    assertEquals("VERIFIED", result.toJsonNode().get(0).get("status").asText());
                    assertEquals("'Aviator status' is already configured correctly.",
                        result.toJsonNode().get(0).get("details").asText());
                }
            }
        }

        @Test
        void testSynchronizeUpdatesExistingCustomTagWhenValuesAreMissing() throws Exception {
            try (var server = new TestSscServer()) {
                String tagId = "1002";
                TagDefinition tagDefinition = AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG;
                String existingValue = tagDefinition.getValues().get(0);
                server.withCustomTags(tagSummary(tagId, tagDefinition));
                server.withCustomTagDetails(tagId, tagDetails(tagId, tagDefinition, existingValue));

                try (var unirest = newUnirest(server)) {
                    var result = new AviatorSSCPrepareHelper.PrepareResult();
                    SynchronizationResult syncResult = new AviatorSSCCustomTagHelper(unirest, tagDefinition)
                        .synchronize(result);

                    assertNotNull(syncResult);
                    assertNotNull(syncResult.getTag());
                    assertEquals(tagId, syncResult.getTag().get("id").asText());
                    assertEquals(tagDefinition.getValues().size(), syncResult.getTag().withArray("valueList").size());
                    assertTrue(hasLookupValue(syncResult.getTag().get("valueList"), "AVIATOR:Unsure"));
                    assertEquals(1, server.getCustomTagsGetCount());
                    assertEquals(1, server.getCustomTagDetailsGetCount());
                    assertEquals(0, server.getCustomTagCreateCount());
                    assertEquals(1, server.getCustomTagUpdateCount());
                    assertNotNull(server.getLastUpdatedTag());
                    assertEquals(tagDefinition.getValues().size(), server.getLastUpdatedTag().withArray("valueList").size());
                    assertEquals("UPDATED", result.toJsonNode().get(0).get("status").asText());
                    assertEquals(
                        "Added 5 missing values to tag 'Aviator prediction'.",
                        result.toJsonNode().get(0).get("details").asText());
                }
            }
        }

        @Test
        void testSynchronizeAddsNewStatusValueToExistingCustomTag() throws Exception {
            try (var server = new TestSscServer()) {
                String tagId = "1003";
                TagDefinition tagDefinition = AviatorSSCTagDefs.AVIATOR_STATUS_TAG;
                server.withCustomTags(tagSummary(tagId, tagDefinition));
                server.withCustomTagDetails(tagId, tagDetails(tagId, tagDefinition, "PROCESSED_BY_AVIATOR"));

                try (var unirest = newUnirest(server)) {
                    var result = new AviatorSSCPrepareHelper.PrepareResult();
                    SynchronizationResult syncResult = new AviatorSSCCustomTagHelper(unirest, tagDefinition)
                        .synchronize(result);

                    assertTrue(syncResult.isSuccessful());
                    assertEquals(2, syncResult.getTag().withArray("valueList").size());
                    assertTrue(hasLookupValue(syncResult.getTag().get("valueList"), "PROCESSED_BY_AVIATOR"));
                    assertTrue(hasLookupValue(syncResult.getTag().get("valueList"),
                            "PROCESSED_BY_AVIATOR_WITH_REMEDIATION"));
                    assertEquals(1, server.getCustomTagUpdateCount());
                    assertEquals(0, server.getCustomTagCreateCount());
                    JsonNode addedValue = findLookupValue(server.getLastUpdatedTag().get("valueList"),
                            "PROCESSED_BY_AVIATOR_WITH_REMEDIATION");
                    assertEquals(2, addedValue.path("lookupIndex").asInt());
                    assertEquals(2, addedValue.path("seqNumber").asInt());
                }
            }
        }

        @Test
        void testSynchronizeCreatesTagWhenAbsentFromBothEndpoints() throws Exception {
            try (var server = new TestSscServer()) {
                server.withCreateResponse(createdTag("2002", AviatorSSCTagDefs.AVIATOR_STATUS_TAG));

                try (var unirest = newUnirest(server)) {
                    var result = new AviatorSSCPrepareHelper.PrepareResult();
                    SynchronizationResult syncResult = new AviatorSSCCustomTagHelper(unirest, AviatorSSCTagDefs.AVIATOR_STATUS_TAG)
                        .synchronize(result);

                    assertNotNull(syncResult);
                    assertNotNull(syncResult.getTag());
                    assertEquals("2002", syncResult.getTag().get("id").asText());
                    assertEquals(1, server.getCustomTagsGetCount());
                    assertEquals(0, server.getCustomTagDetailsGetCount());
                    assertEquals(1, server.getCustomTagCreateCount());
                    assertEquals(0, server.getCustomTagUpdateCount());
                    assertEquals("CREATED", result.toJsonNode().get(0).get("status").asText());
                    assertEquals("Tag 'Aviator status' created successfully.",
                        result.toJsonNode().get(0).get("details").asText());
                    assertTrue(hasLookupValue(server.getLastCreatedTag().get("valueList"),
                            "PROCESSED_BY_AVIATOR"));
                    assertTrue(hasLookupValue(server.getLastCreatedTag().get("valueList"),
                            "PROCESSED_BY_AVIATOR_WITH_REMEDIATION"));
                }
            }
        }

        @Test
        void testSystemManagedStatusTagIsNotModifiedEvenWhenListedAsCustom() throws Exception {
            try (var server = new TestSscServer()) {
                String tagId = "3001";
                server.withInternalTags(systemManagedTag(tagId, AviatorSSCTagDefs.AVIATOR_STATUS_TAG));
                server.withCustomTags(tagSummary(tagId, AviatorSSCTagDefs.AVIATOR_STATUS_TAG));
                server.withCustomTagDetails(tagId, tagDetails(tagId, AviatorSSCTagDefs.AVIATOR_STATUS_TAG,
                        "PROCESSED_BY_AVIATOR"));

                try (var unirest = newUnirest(server)) {
                    var result = new AviatorSSCPrepareHelper.PrepareResult();
                    SynchronizationResult syncResult = new AviatorSSCCustomTagHelper(
                            unirest, AviatorSSCTagDefs.AVIATOR_STATUS_TAG).synchronize(result);

                    assertTrue(syncResult.isSystemManaged());
                    assertFalse(syncResult.requiresAssociation());
                    assertEquals(1, server.getInternalTagsGetCount());
                    assertEquals(0, server.getCustomTagsGetCount());
                    assertEquals(0, server.getCustomTagDetailsGetCount());
                    assertEquals(0, server.getCustomTagCreateCount());
                    assertEquals(0, server.getCustomTagUpdateCount());
                    assertEquals("SYSTEM_MANAGED", result.toJsonNode().get(0).get("status").asText());
                }
            }
        }

        @ParameterizedTest
        @ValueSource(ints = {404, 405})
        void testUnsupportedInternalCatalogUsesCustomTagPath(int status) throws Exception {
            try (var server = new TestSscServer().withInternalTagsFailure(status)) {
                server.withCreateResponse(createdTag("2003", AviatorSSCTagDefs.AVIATOR_STATUS_TAG));

                try (var unirest = newUnirest(server)) {
                    var result = new AviatorSSCPrepareHelper.PrepareResult();
                    SynchronizationResult syncResult = new AviatorSSCCustomTagHelper(
                            unirest, AviatorSSCTagDefs.AVIATOR_STATUS_TAG).synchronize(result);

                    assertTrue(syncResult.isSuccessful());
                    assertFalse(syncResult.isSystemManaged());
                    assertEquals(1, server.getInternalTagsGetCount());
                    assertEquals(1, server.getCustomTagsGetCount());
                    assertEquals(1, server.getCustomTagCreateCount());
                    assertEquals("CREATED", result.toJsonNode().get(0).get("status").asText());
                }
            }
        }

        @Test
        void testInternalCatalogWithoutDataArrayFails() throws Exception {
            try (var server = new TestSscServer().withoutInternalTagsData()) {
                try (var unirest = newUnirest(server)) {
                    var result = new AviatorSSCPrepareHelper.PrepareResult();
                    SynchronizationResult syncResult = new AviatorSSCCustomTagHelper(
                            unirest, AviatorSSCTagDefs.AVIATOR_STATUS_TAG).synchronize(result);

                    assertFalse(syncResult.isSuccessful());
                    assertEquals(0, server.getCustomTagsGetCount());
                    assertEquals(0, server.getCustomTagCreateCount());
                    assertEquals("FAILED", result.toJsonNode().get(0).get("status").asText());
                }
            }
        }

        @Test
        void testInternalCatalogFailureDoesNotFallBackToCustomTagMutation() throws Exception {
            try (var server = new TestSscServer().withInternalTagsFailure(500)) {
                try (var unirest = newUnirest(server)) {
                    var result = new AviatorSSCPrepareHelper.PrepareResult();
                    SynchronizationResult syncResult = new AviatorSSCCustomTagHelper(
                            unirest, AviatorSSCTagDefs.AVIATOR_STATUS_TAG).synchronize(result);

                    assertFalse(syncResult.isSuccessful());
                    assertEquals(1, server.getInternalTagsGetCount());
                    assertEquals(0, server.getCustomTagsGetCount());
                    assertEquals(0, server.getCustomTagCreateCount());
                    assertEquals(0, server.getCustomTagUpdateCount());
                    assertEquals("FAILED", result.toJsonNode().get(0).get("status").asText());
                }
            }
        }

        private UnirestInstance newUnirest(TestSscServer server) {
            return UnirestHelper.createUnirestInstance(unirest -> {
                UnirestJsonHeaderConfigurer.configure(unirest);
                UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
                unirest.config().defaultBaseUrl(server.getBaseUrl());
            });
        }

        private static ObjectNode tagSummary(String id, TagDefinition tagDefinition) {
            return JsonHelper.getObjectMapper().createObjectNode()
                .put("id", id)
                .put("guid", tagDefinition.getGuid())
                .put("name", tagDefinition.getName());
        }

        private static ObjectNode tagDetails(String id, TagDefinition tagDefinition) {
            return tagDetails(id, tagDefinition, tagDefinition.getValues().toArray(String[]::new));
        }

        private static ObjectNode tagDetails(String id, TagDefinition tagDefinition, String... values) {
            ObjectNode result = tagSummary(id, tagDefinition)
                .put("valueType", "LIST")
                .put("customTagType", "CUSTOM");
            ArrayNode valueList = result.putArray("valueList");
            for (int i = 0; i < values.length; i++) {
                valueList.add(JsonHelper.getObjectMapper().createObjectNode()
                        .put("lookupValue", values[i])
                        .put("lookupIndex", i + 1)
                        .put("seqNumber", i + 1));
            }
            return result;
        }

        private static ObjectNode systemManagedTag(String id, TagDefinition tagDefinition) {
            ObjectNode tag = tagDetails(id, tagDefinition, tagDefinition.getValues().toArray(String[]::new));
            tag.put("customTagType", "AVIATOR");
            return tag;
        }

        private static ObjectNode createdTag(String id, TagDefinition tagDefinition) {
            return tagSummary(id, tagDefinition)
                .put("valueType", "LIST")
                .put("customTagType", "CUSTOM");
        }

        private static boolean hasLookupValue(JsonNode valueList, String lookupValue) {
            for (JsonNode value : valueList) {
                if (lookupValue.equals(value.path("lookupValue").asText())) {
                    return true;
                }
            }
            return false;
        }

        private static JsonNode findLookupValue(JsonNode valueList, String lookupValue) {
            for (JsonNode value : valueList) {
                if (lookupValue.equals(value.path("lookupValue").asText())) {
                    return value;
                }
            }
            throw new AssertionError("Value not found: " + lookupValue);
        }
    }

    private static final class TestSscServer implements AutoCloseable {
        private final HttpServer server;
        private final ArrayNode customTags = JsonHelper.getObjectMapper().createArrayNode();
        private final ArrayNode internalTags = JsonHelper.getObjectMapper().createArrayNode();
        private final Map<String, JsonNode> customTagDetailsById = new HashMap<>();
        private JsonNode createResponse = JsonHelper.getObjectMapper().createObjectNode();
        private JsonNode lastCreatedTag;
        private int internalTagsGetCount;
        private int customTagsGetCount;
        private int customTagDetailsGetCount;
        private int customTagCreateCount;
        private int customTagUpdateCount;
        private JsonNode lastUpdatedTag;

        private TestSscServer() throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/customTags", this::handleCustomTags);
            server.createContext("/api/v1/customTags/", this::handleCustomTagById);
            server.createContext("/api/v1/internalCustomTags", this::handleInternalCustomTags);
            server.start();
        }

        private String getBaseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        private TestSscServer withCustomTags(JsonNode... tags) {
            customTags.removeAll();
            for (JsonNode tag : tags) {
                customTags.add(tag);
            }
            return this;
        }

        private TestSscServer withCustomTagDetails(String tagId, JsonNode tagDetails) {
            customTagDetailsById.put(tagId, tagDetails);
            return this;
        }

        private TestSscServer withCreateResponse(JsonNode tag) {
            this.createResponse = tag;
            return this;
        }

        private TestSscServer withInternalTags(JsonNode... tags) {
            internalTagsFailureStatus = 0;
            internalTags.removeAll();
            for (JsonNode tag : tags) {
                internalTags.add(tag);
            }
            return this;
        }

        private TestSscServer withoutInternalTagsData() {
            this.internalTagsFailureStatus = 0;
            this.omitInternalTagsData = true;
            return this;
        }

        private TestSscServer withInternalTagsFailure(int status) {
            this.internalTagsFailureStatus = status;
            return this;
        }

        private int internalTagsFailureStatus = 404;
        private boolean omitInternalTagsData;
        private int getInternalTagsGetCount() { return internalTagsGetCount; }
        private int getCustomTagsGetCount() { return customTagsGetCount; }
        private int getCustomTagDetailsGetCount() { return customTagDetailsGetCount; }
        private int getCustomTagCreateCount() { return customTagCreateCount; }
        private int getCustomTagUpdateCount() { return customTagUpdateCount; }
        private JsonNode getLastUpdatedTag() { return lastUpdatedTag; }
        private JsonNode getLastCreatedTag() { return lastCreatedTag; }

        private void handleInternalCustomTags(HttpExchange exchange) throws IOException {
            internalTagsGetCount++;
            if (internalTagsFailureStatus != 0) {
                exchange.sendResponseHeaders(internalTagsFailureStatus, -1);
                exchange.close();
                return;
            }
            if (omitInternalTagsData) {
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
                return;
            }
            writeJson(exchange, internalTags);
        }

        private void handleCustomTags(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                customTagsGetCount++;
                writeJson(exchange, customTags);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                customTagCreateCount++;
                lastCreatedTag = JsonHelper.getObjectMapper().readTree(exchange.getRequestBody());
                writeJson(exchange, createResponse);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }

        private void handleCustomTagById(HttpExchange exchange) throws IOException {
            String prefix = "/api/v1/customTags/";
            String tagId = exchange.getRequestURI().getPath().substring(prefix.length());
            if ("GET".equals(exchange.getRequestMethod())) {
                customTagDetailsGetCount++;
                JsonNode tagDetails = customTagDetailsById.get(tagId);
                if (tagDetails == null) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                writeJson(exchange, tagDetails);
            } else if ("PUT".equals(exchange.getRequestMethod())) {
                customTagUpdateCount++;
                lastUpdatedTag = JsonHelper.getObjectMapper().readTree(exchange.getRequestBody());
                customTagDetailsById.put(tagId, lastUpdatedTag);
                writeJson(exchange, lastUpdatedTag);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }

        private void writeJson(HttpExchange exchange, JsonNode data) throws IOException {
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
