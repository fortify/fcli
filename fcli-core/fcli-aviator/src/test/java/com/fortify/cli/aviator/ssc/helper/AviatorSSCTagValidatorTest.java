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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;

/**
 * Pre-upload checks for Aviator prediction, Aviator status, and Analysis values.
 * One {@code includeall=true} response supplies assigned {@code CUSTOM} tags and built-in
 * {@code AVIATOR} tags. A missing tag uses the internal catalog to choose enable-Aviator
 * or prepare guidance.
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
    @DisplayName("assigned CUSTOM Aviator tags in the includeall response pass")
    void assignedCustomTagsDoNotWarn() throws IOException {
        server = new TestSscServer()
            .withTags(customTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                customTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"))
            .withCustomTagDetails(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(),
                tagDetails(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "PROCESSED_BY_AVIATOR",
                    "PROCESSED_BY_AVIATOR_WITH_REMEDIATION"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.isEmpty());
        assertTrue(server.includeAllRequests == 1);
        assertTrue(server.statusTagDetailsRequests == 1);
        assertTrue(server.plainRequests == 0);
        assertTrue(server.internalCatalogRequests == 0);
    }

    @Test
    @DisplayName("user-managed status tag missing the remediation value warns to prepare SSC")
    void userManagedStatusTagMissingRemediationValueWarns() throws IOException {
        String statusGuid = AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid();
        server = new TestSscServer()
            .withTags(
                customTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                customTag(statusGuid, "Aviator status"))
            .withCustomTagDetails(statusGuid, tagDetails(statusGuid, "PROCESSED_BY_AVIATOR"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, new CollectingLogger()));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("PROCESSED_BY_AVIATOR_WITH_REMEDIATION")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("fcli aviator ssc prepare")));
    }

    @Test
    @DisplayName("system-managed status tag uses the recognized legacy value and warns about lost distinction")
    void systemManagedStatusTagMissingRemediationValueUsesLegacyStatus() throws IOException {
        ObjectNode statusTag = aviatorStatusTag("PROCESSED_BY_AVIATOR");
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                statusTag);
        unirest = newUnirest(server);

        CollectingLogger decisionLogger = new CollectingLogger();
        assertTrue(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, decisionLogger));
        assertTrue(decisionLogger.warnings.isEmpty(), decisionLogger.warnings.toString());

        List<String> warnings = validate(AviatorSSCTagValidator.sastStatusValues(true));

        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    @DisplayName("legacy status values exclude the remediation value; default SAST values include both")
    void sastStatusValuesFollowLegacyDecision() {
        assertEquals(List.of(Constants.PROCESSED_BY_AVIATOR), AviatorSSCTagValidator.sastStatusValues(true));
        assertEquals(Constants.AVIATOR_STATUS_VALUES, AviatorSSCTagValidator.sastStatusValues(false));
    }

    @Test
    @DisplayName("legacy status fallback warning reports the count and preserved remediation data")
    void legacyStatusFallbackWarningIsClear() {
        String warning = AviatorSSCTagValidator.legacyStatusFallbackWarning(3);

        assertTrue(warning.startsWith("WARN: 3 issue(s) with remediation were marked 'PROCESSED_BY_AVIATOR'"), warning);
        assertTrue(warning.contains("instead of 'PROCESSED_BY_AVIATOR_WITH_REMEDIATION'"), warning);
        assertTrue(warning.contains("The fallback does not change remediation generation"), warning);
        assertTrue(warning.contains("will not distinguish"), warning);
        assertFalse(warning.contains("%"), warning);
    }

    @Test
    @DisplayName("status tag missing from the version does not use the fallback")
    void missingStatusTagDoesNotUseLegacyStatus() throws IOException {
        server = new TestSscServer()
            .withTags(aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"));
        unirest = newUnirest(server);

        CollectingLogger logger = new CollectingLogger();
        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, logger));
        assertTrue(logger.warnings.isEmpty(), logger.warnings.toString());
        assertEquals(0, server.internalCatalogRequests);
    }

    @Test
    @DisplayName("system-managed status values from the internal catalog drive the fallback decision")
    void systemManagedStatusValuesFromInternalCatalogUseLegacyStatus() throws IOException {
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"))
            .withInternalCatalog(aviatorStatusTag("PROCESSED_BY_AVIATOR"));
        unirest = newUnirest(server);

        assertTrue(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, new CollectingLogger()));
        assertEquals(1, server.internalCatalogRequests);
    }

    @Test
    @DisplayName("internal catalog entry that is not system-managed is unknown capability and warns")
    void internalCatalogStatusEntryNotSystemManagedWarns() throws IOException {
        ObjectNode catalogEntry = aviatorStatusTag("PROCESSED_BY_AVIATOR");
        catalogEntry.put("customTagType", "CUSTOM");
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"))
            .withInternalCatalog(catalogEntry);
        unirest = newUnirest(server);

        CollectingLogger logger = new CollectingLogger();
        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, logger));
        assertTrue(logger.warnings.stream().anyMatch(w -> w.contains("Could not check the values")), logger.warnings.toString());
    }

    @Test
    @DisplayName("internal catalog HTTP failure during the fallback decision warns and continues")
    void internalCatalogFailureDuringDecisionWarns() throws IOException {
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"))
            .failInternalCatalog(500);
        unirest = newUnirest(server);

        CollectingLogger logger = new CollectingLogger();
        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, logger));
        assertEquals(1, logger.warnings.size(), logger.warnings.toString());
        assertTrue(logger.warnings.get(0).contains("Could not check SSC 'Aviator status' support"));
        assertTrue(logger.warnings.get(0).contains("PROCESSED_BY_AVIATOR_WITH_REMEDIATION"));
    }

    @Test
    @DisplayName("internal catalog HTTP failure during status value validation warns and continues")
    void internalCatalogFailureDuringValidationWarns() throws IOException {
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"))
            .failInternalCatalog(500);
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("Pre-upload tag validation failed"));
    }

    @Test
    @DisplayName("includeall response without a data array is unknown capability and warns")
    void includeAllWithoutDataDuringDecisionWarns() throws IOException {
        server = new TestSscServer().withoutIncludeAllData();
        unirest = newUnirest(server);

        CollectingLogger logger = new CollectingLogger();
        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, logger));
        assertTrue(logger.warnings.stream().anyMatch(w -> w.contains("SSC returned no custom tags")),
            logger.warnings.toString());
    }

    @Test
    @DisplayName("system-managed status tag with the new value does not use the legacy fallback")
    void systemManagedStatusTagSupportingRemediationValueKeepsNewStatus() throws IOException {
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                aviatorStatusTag("PROCESSED_BY_AVIATOR", "PROCESSED_BY_AVIATOR_WITH_REMEDIATION"));
        unirest = newUnirest(server);

        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, new CollectingLogger()));
        assertTrue(validate().isEmpty());
    }

    @Test
    @DisplayName("unknown system-managed status value list does not guess a fallback")
    void unknownSystemManagedStatusValueListKeepsWarnAndContinueBehavior() throws IOException {
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"))
            .withInternalCatalog(
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"));
        unirest = newUnirest(server);

        CollectingLogger logger = new CollectingLogger();
        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, logger));
        assertTrue(logger.warnings.stream().anyMatch(w -> w.contains("Could not check the values of the system-managed")));
        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Could not verify the value list for 'Aviator status'")));
    }

    @Test
    @DisplayName("system-managed status tag without the legacy value does not use the fallback")
    void missingLegacyStatusValueKeepsWarnAndContinueBehavior() throws IOException {
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                aviatorStatusTag("PROCESSED_BY_AVIATOR_WITH_REMEDIATION"));
        unirest = newUnirest(server);

        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, new CollectingLogger()));
        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("PROCESSED_BY_AVIATOR")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("will not distinguish")));
    }

    @Test
    @DisplayName("DAST validation does not warn about the SAST remediation status value")
    void dastValidationDoesNotWarnForMissingRemediationStatusValue() throws IOException {
        String statusGuid = AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid();
        server = new TestSscServer()
            .withTags(
                customTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                customTag(statusGuid, "Aviator status"))
            .withCustomTagDetails(statusGuid, tagDetails(statusGuid, "PROCESSED_BY_AVIATOR"));
        unirest = newUnirest(server);

        List<String> warnings = validate(List.of());

        assertFalse(warnings.stream().anyMatch(w -> w.contains("PROCESSED_BY_AVIATOR_WITH_REMEDIATION")));
        assertEquals(0, server.statusTagDetailsRequests);
    }

    @Test
    @DisplayName("built-in AVIATOR tags in the includeall response do not warn")
    void builtInAviatorTagsDoNotWarn() throws IOException {
        server = new TestSscServer()
            .withTags(
                customTag(ANALYSIS_GUID, "Analysis"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.isEmpty(), warnings.toString());
        assertTrue(server.includeAllRequests == 1);
        assertTrue(server.plainRequests == 0);
        assertTrue(server.internalCatalogRequests == 0);
    }

    @Test
    @DisplayName("system-managed status values are checked from the internal catalog")
    void systemManagedStatusValuesUseInternalCatalogWhenVersionTagOmitsValueList() throws IOException {
        server = new TestSscServer()
            .withTags(
                tag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction", "AVIATOR"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"))
            .withBuiltInAviatorCatalog();
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(1, server.internalCatalogRequests);
    }

    @Test
    @DisplayName("missing system-managed status value in the internal catalog warns")
    void systemManagedStatusValueMissingFromInternalCatalogWarns() throws IOException {
        ObjectNode statusTag = tag(
            AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR");
        statusTag.putArray("valueList")
            .add(JsonHelper.getObjectMapper().createObjectNode().put("lookupValue", "PROCESSED_BY_AVIATOR"));
        server = new TestSscServer()
            .withTags(
                tag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction", "AVIATOR"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"))
            .withInternalCatalog(
                tag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction", "AVIATOR"),
                statusTag);
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("PROCESSED_BY_AVIATOR_WITH_REMEDIATION")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Configure the system-managed tag through SSC")));
    }

    @Test
    @DisplayName("unavailable system-managed status value list warns and continues")
    void unavailableSystemManagedStatusValueListWarns() throws IOException {
        server = new TestSscServer()
            .withTags(
                tag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction", "AVIATOR"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"))
            .withInternalCatalog(
                tag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction", "AVIATOR"),
                tag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Could not verify the value list for 'Aviator status'")));
        assertEquals(1, server.internalCatalogRequests);
    }

    @Test
    @DisplayName("SSC 26.2+ missing Aviator tags say to enable Aviator")
    void ssc26MissingAviatorTagsSayEnableAviator() throws IOException {
        server = new TestSscServer()
            .withBuiltInAviatorCatalog()
            .withTags(customTag(ANALYSIS_GUID, "Analysis"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator prediction")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator status")));
        assertTrue(warnings.stream().allMatch(w -> w.contains("Enable Aviator in SSC")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("fcli aviator ssc prepare")));
        assertTrue(server.includeAllRequests == 1);
        assertTrue(server.plainRequests == 0);
    }

    @Test
    @DisplayName("internal catalog without Aviator tags keeps the prepare warning")
    void internalCatalogWithoutAviatorTagsKeepsPrepare() throws IOException {
        server = new TestSscServer()
            .withInternalCatalog(customTag(ANALYSIS_GUID, "Analysis"))
            .withTags(customTag(ANALYSIS_GUID, "Analysis"));
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator prediction")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Aviator status")));
        assertTrue(warnings.stream().allMatch(w -> w.contains("fcli aviator ssc prepare")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Enable Aviator")));
        assertTrue(server.plainRequests == 0);
    }

    @Test
    @DisplayName("Analysis in the includeall response is accepted")
    void analysisTagInIncludeAllDoesNotWarn() throws IOException {
        server = new TestSscServer()
            .withTags(
                customTag(ANALYSIS_GUID, "Analysis"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"));
        unirest = newUnirest(server);

        List<String> warnings = validate(ANALYSIS_GUID, Set.of("Not an Issue"));

        assertTrue(warnings.isEmpty(), warnings.toString());
        assertTrue(server.includeAllRequests == 1);
        assertTrue(server.plainRequests == 0);
    }

    @Test
    @DisplayName("missing Analysis tag warns while built-in Aviator tags pass")
    void missingAnalysisTagWarns() throws IOException {
        server = new TestSscServer()
            .withTags(
                aviatorTag(AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG.getGuid(), "Aviator prediction"),
                aviatorTag(AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status"));
        unirest = newUnirest(server);

        List<String> warnings = validate(ANALYSIS_GUID, Set.of("Not an Issue"));

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Analysis tag")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Aviator prediction")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Aviator status")));
        assertTrue(server.plainRequests == 0);
    }

    @Test
    @DisplayName("includeall failure reports that validation failed")
    void includeAllFailureReportsValidationFailed() throws IOException {
        server = new TestSscServer().failIncludeAll();
        unirest = newUnirest(server);

        List<String> warnings = validate();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("Pre-upload tag validation failed")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("fcli aviator ssc prepare")));
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Enable Aviator")));
        assertTrue(server.includeAllRequests == 1);
        assertTrue(server.plainRequests == 0);
    }

    @Test
    @DisplayName("status capability lookup failure warns before auditing")
    void statusCapabilityLookupFailureWarns() throws IOException {
        server = new TestSscServer().failIncludeAll();
        unirest = newUnirest(server);

        CollectingLogger logger = new CollectingLogger();
        assertFalse(AviatorSSCTagValidator.shouldUseLegacySastStatus(unirest, VERSION_ID, logger));
        assertTrue(logger.warnings.stream().anyMatch(w -> w.contains("Could not check SSC 'Aviator status' support")));
    }

    private List<String> validate() {
        return validate(null, Set.of());
    }

    private List<String> validate(String analysisTagId, Set<String> analysisTagValues) {
        return validate(AviatorSSCTagValidator.sastStatusValues(false), analysisTagId, analysisTagValues);
    }

    private List<String> validate(List<String> statusValues) {
        return validate(statusValues, null, Set.of());
    }

    private List<String> validate(List<String> statusValues, String analysisTagId,
            Set<String> analysisTagValues) {
        CollectingLogger logger = new CollectingLogger();
        List<String> warnings = AviatorSSCTagValidator.validatePreUpload(
            unirest, VERSION_ID, analysisTagId, analysisTagValues, statusValues, logger);
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
        ObjectNode result = tag(guid, name, "AVIATOR");
        if (AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid().equalsIgnoreCase(guid)) {
            ArrayNode values = result.putArray("valueList");
            values.add(JsonHelper.getObjectMapper().createObjectNode().put("lookupValue", "PROCESSED_BY_AVIATOR"));
            values.add(JsonHelper.getObjectMapper().createObjectNode()
                .put("lookupValue", "PROCESSED_BY_AVIATOR_WITH_REMEDIATION"));
        }
        return result;
    }

    private static ObjectNode aviatorStatusTag(String... values) {
        ObjectNode result = tag(
            AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid(), "Aviator status", "AVIATOR");
        ArrayNode valueList = result.putArray("valueList");
        for (String value : values) {
            valueList.add(JsonHelper.getObjectMapper().createObjectNode().put("lookupValue", value));
        }
        return result;
    }

    private static ObjectNode tag(String guid, String name, String customTagType) {
        return JsonHelper.getObjectMapper().createObjectNode()
            .put("id", guid)
            .put("guid", guid)
            .put("name", name)
            .put("customTagType", customTagType);
    }

    private static ObjectNode tagDetails(String id, String... values) {
        ObjectNode details = JsonHelper.getObjectMapper().createObjectNode()
            .put("id", id)
            .put("name", "Aviator status")
            .put("guid", AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid())
            .put("customTagType", "CUSTOM")
            .put("valueType", "LIST");
        ArrayNode valueList = details.putArray("valueList");
        for (String value : values) {
            valueList.add(JsonHelper.getObjectMapper().createObjectNode().put("lookupValue", value));
        }
        return details;
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
        private final ArrayNode tags = JsonHelper.getObjectMapper().createArrayNode();
        private final ArrayNode internalCatalog = JsonHelper.getObjectMapper().createArrayNode();
        private final java.util.Map<String, ObjectNode> customTagDetails = new java.util.HashMap<>();
        private boolean failIncludeAll;
        private boolean omitIncludeAllData;
        private int internalCatalogFailureStatus;
        private boolean internalCatalogPresent;
        private int includeAllRequests;
        private int plainRequests;
        private int internalCatalogRequests;
        private int statusTagDetailsRequests;

        private TestSscServer() throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/projectVersions/" + VERSION_ID + "/customTags", this::handleCustomTags);
            server.createContext("/api/v1/internalCustomTags", this::handleInternalCustomTags);
            server.createContext("/api/v1/customTags/", this::handleCustomTagDetails);
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

        private TestSscServer withTags(ObjectNode... versionTags) {
            tags.removeAll();
            for (ObjectNode tag : versionTags) {
                tags.add(tag);
            }
            return this;
        }

        private TestSscServer withCustomTagDetails(String id, ObjectNode details) {
            customTagDetails.put(id, details);
            return this;
        }

        private TestSscServer failIncludeAll() {
            this.failIncludeAll = true;
            return this;
        }

        private TestSscServer withoutIncludeAllData() {
            this.omitIncludeAllData = true;
            return this;
        }

        private TestSscServer failInternalCatalog(int status) {
            this.internalCatalogFailureStatus = status;
            return this;
        }

        private void handleInternalCustomTags(HttpExchange exchange) throws IOException {
            internalCatalogRequests++;
            if (internalCatalogFailureStatus != 0) {
                exchange.sendResponseHeaders(internalCatalogFailureStatus, -1);
                exchange.close();
                return;
            }
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
                if (omitIncludeAllData) {
                    writeJson(exchange, null);
                    return;
                }
                writeJson(exchange, tags);
                return;
            }
            plainRequests++;
            writeJson(exchange, JsonHelper.getObjectMapper().createArrayNode());
        }

        private void handleCustomTagDetails(HttpExchange exchange) throws IOException {
            statusTagDetailsRequests++;
            String id = exchange.getRequestURI().getPath().substring("/api/v1/customTags/".length());
            ObjectNode details = customTagDetails.get(id);
            if (details == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            writeJson(exchange, details);
        }

        private void writeJson(HttpExchange exchange, JsonNode data) throws IOException {
            ObjectNode wrapper = JsonHelper.getObjectMapper().createObjectNode();
            if (data != null) {
                wrapper.set("data", data);
            }
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
