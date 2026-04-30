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

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAttributeDefs.AttributeDefinition;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeUpdateBuilder;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Manages SSC attribute definitions used by the Aviator module.
 *
 * <p>Mirrors the pattern in {@link AviatorSSCCustomTagHelper} but operates on
 * per-application-version attributes ({@code /api/v1/attributeDefinitions} +
 * {@code /api/v1/projectVersions/{id}/attributes}) rather than per-issue
 * custom tags.
 */
@RequiredArgsConstructor
public class AviatorSSCAttributeHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCAttributeHelper.class);
    private final UnirestInstance unirest;
    private final AttributeDefinition attrDef;

    /**
     * Ensures the attribute definition exists on the SSC instance.
     *
     * <ul>
     *   <li>If already present (matched by GUID): logs VERIFIED and returns.
     *   <li>If absent: creates it via {@code POST /api/v1/attributeDefinitions}.
     * </ul>
     *
     * @param result sink for user-visible status entries
     */
    public void synchronize(AviatorSSCPrepareHelper.PrepareResult result) {
        try {
            LOG.debug("Searching for attribute definition '{}' (GUID: {})", attrDef.name(), attrDef.guid());
            if (findDefinition() != null) {
                LOG.info("Attribute definition '{}' is already present.", attrDef.name());
                result.addEntry("Attribute Definition", "VERIFIED",
                    "'" + attrDef.name() + "' is already present on this SSC instance.");
            } else {
                createDefinition();
                LOG.info("Attribute definition '{}' created successfully.", attrDef.name());
                result.addEntry("Attribute Definition", "CREATED",
                    "Attribute definition '" + attrDef.name() + "' created successfully.");
            }
        } catch (UnexpectedHttpResponseException | FcliSimpleException e) {
            LOG.error("Error synchronizing attribute definition '{}': {}", attrDef.name(), e.getMessage());
            result.addEntry("Attribute Definition", "WARNING",
                "Failed to synchronize attribute definition '" + attrDef.name() + "': " + e.getMessage()
                    + ". The correlate-sast-dast command will attempt to create it on first use.");
        }
    }

    /**
     * Writes the current UTC timestamp to the {@code last_correlation} attribute on
     * the given application version.
     *
     * <p>If the attribute definition does not yet exist (e.g. {@code prepare} was never
     * run), this method automatically creates it before writing.
     *
     * @param unirest   active SSC session
     * @param versionId SSC project version ID
     */
    public static void writeLastCorrelationTimestamp(UnirestInstance unirest, String versionId) {
        var helper = new AviatorSSCAttributeHelper(unirest, AviatorSSCAttributeDefs.LAST_CORRELATION_ATTR);
        helper.ensureDefinitionExists();

        String timestamp = Instant.now().toString();
        LOG.debug("Writing last_correlation timestamp '{}' to app version {}", timestamp, versionId);

        new SSCAttributeUpdateBuilder(unirest)
            .add(Map.of(AviatorSSCAttributeDefs.LAST_CORRELATION_ATTR.name(), timestamp))
            .buildRequest(versionId)
            .asObject(JsonNode.class);

        LOG.info("last_correlation timestamp '{}' written to app version {}", timestamp, versionId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the attribute definition node matched by name, or {@code null} if absent.
     * SSC auto-generates the GUID on create (it is not part of the POST request body),
     * so name is the only stable lookup key.
     */
    private JsonNode findDefinition() {
        JsonNode responseBody = unirest.get(SSCUrls.ATTRIBUTE_DEFINITIONS + "?limit=-1")
            .asObject(JsonNode.class).getBody();
        JsonNode data = responseBody.get("data");
        if (data == null || !data.isArray()) return null;
        return JsonHelper.stream((ArrayNode) data)
            .filter(n -> attrDef.name().equals(n.path("name").asText()))
            .findFirst().orElse(null);
    }

    /** Creates the attribute definition via {@code POST /api/v1/attributeDefinitions}. */
    private void createDefinition() {
        ObjectNode payload = buildCreatePayload();
        LOG.debug("Creating attribute definition '{}': {}", attrDef.name(), payload.toPrettyString());
        unirest.post(SSCUrls.ATTRIBUTE_DEFINITIONS)
            .body(payload)
            .asObject(JsonNode.class)
            .getBody();
    }

    /** Ensures the definition exists — used by {@link #writeLastCorrelationTimestamp}. */
    private void ensureDefinitionExists() {
        if (findDefinition() == null) {
            LOG.info("Attribute definition '{}' not found — creating before write.", attrDef.name());
            createDefinition();
        }
    }

    private ObjectNode buildCreatePayload() {
        ObjectNode node = JsonHelper.getObjectMapper().createObjectNode();
        // Exact fields validated against SSC UI capture — see comments for each.
        // guid: NOT sent — SSC auto-generates it; sending a custom guid causes HTTP 500.
        // options: NOT sent — null in SSC response for TEXT type; empty array causes HTTP 500.
        node.put("name",          attrDef.name());
        node.put("description",   attrDef.description());
        node.put("category",      attrDef.category());   // must be UPPERCASE, e.g. "TECHNICAL"
        node.put("type",          attrDef.type());
        node.put("appEntityType", "PROJECT_VERSION");     // required discriminator — missing this causes HTTP 500
        return node;
    }
}
