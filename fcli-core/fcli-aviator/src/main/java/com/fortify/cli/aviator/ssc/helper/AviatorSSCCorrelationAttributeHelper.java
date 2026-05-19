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
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelationAttributeDefs.AttributeDefinition;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeUpdateBuilder;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Manages the SSC attribute definitions used by the SAST-DAST correlation feature.
 *
 * <p>The attribute definition is created by {@code aviator ssc prepare} (admin-only).
 * The attribute value is written per application version by
 * {@code aviator ssc correlate-sast-dast} (non-admin).
 *
 * <p>This is distinct from the generic SSC attribute helpers in the SSC module
 * ({@code SSCAttributeHelper}, {@code SSCAttributeDefinitionHelper}) which
 * handle reading/updating existing attributes. This class also handles
 * <em>creating</em> attribute definitions specific to correlation.
 */
@RequiredArgsConstructor
public class AviatorSSCCorrelationAttributeHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCCorrelationAttributeHelper.class);
    private final UnirestInstance unirest;
    private final AttributeDefinition attrDef;

    /**
     * Ensures the attribute definition exists on the SSC instance.
     * Called by {@code aviator ssc prepare} which requires admin privileges.
     *
     * <ul>
     *   <li>If already present (matched by name): logs VERIFIED and returns.
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
                    + ". Run 'fcli aviator ssc prepare' with admin privileges to create it.");
        }
    }

    /**
     * Writes the current UTC timestamp to the {@code last_correlation} attribute on
     * the given application version.
     *
     * <p>This method assumes the attribute definition already exists — it must have
     * been created by a prior {@code aviator ssc prepare} run. If the definition
     * does not exist, SSC will reject the update and an error is thrown.
     *
     * @param unirest   active SSC session
     * @param versionId SSC project version ID
     */
    public static void writeLastCorrelationTimestamp(UnirestInstance unirest, String versionId) {
        String timestamp = Instant.now().toString();
        LOG.debug("Writing last_correlation timestamp '{}' to app version {}", timestamp, versionId);

        try {
            new SSCAttributeUpdateBuilder(unirest)
                .add(Map.of(AviatorSSCCorrelationAttributeDefs.LAST_CORRELATION_ATTR.name(), timestamp))
                .buildRequest(versionId)
                .asObject(JsonNode.class);

            LOG.info("last_correlation timestamp '{}' written to app version {}", timestamp, versionId);
        } catch (FcliSimpleException e) {
            LOG.warn("WARN: Could not write last_correlation timestamp. Run 'fcli aviator ssc prepare' to create the attribute definition.");
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the attribute definition node matched by name, or {@code null} if absent.
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

    private ObjectNode buildCreatePayload() {
        ObjectNode node = JsonHelper.getObjectMapper().createObjectNode();
        node.put("name",          attrDef.name());
        node.put("description",   attrDef.description());
        node.put("category",      attrDef.category());
        node.put("type",          attrDef.type());
        node.put("appEntityType", "PROJECT_VERSION");
        return node;
    }
}
