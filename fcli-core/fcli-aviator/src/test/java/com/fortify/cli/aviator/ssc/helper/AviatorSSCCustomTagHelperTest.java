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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCustomTagHelper.SynchronizationResult;

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
            assertEquals(1, tag.getValues().size());
            assertTrue(tag.getValues().contains("PROCESSED_BY_AVIATOR"));
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
}
