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
package com.fortify.cli.fod.attribute.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDDataHelper;
import com.fortify.cli.fod._common.util.FoDEnums;

import kong.unirest.UnirestInstance;
import lombok.Getter;

/**
 * Instance-based helper for FoD attribute definition operations. Lazily loads all attribute
 * definitions on first use and caches them for the lifetime of this instance. Intended to be
 * instantiated once per command execution; never stored statically.
 */
public class FoDAttributeDefinitionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FoDAttributeDefinitionHelper.class);
    private final UnirestInstance unirest;

    @Getter(lazy = true)
    private final List<FoDAttributeDefinitionDescriptor> allDefinitions = loadAllDefinitions();

    public FoDAttributeDefinitionHelper(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    private List<FoDAttributeDefinitionDescriptor> loadAllDefinitions() {
        var body = unirest.get(FoDUrls.ATTRIBUTES).asObject(ObjectNode.class).getBody();
        var items = body.get("items");
        if (items == null || !items.isArray()) { return Collections.emptyList(); }
        List<FoDAttributeDefinitionDescriptor> result = new ArrayList<>();
        for (var item : items) {
            var def = JsonHelper.treeToValue(item, FoDAttributeDefinitionDescriptor.class);
            if (def != null) { result.add(def); }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Looks up an attribute definition by name or numeric id, searching the lazy-loaded list.
     */
    public FoDAttributeDefinitionDescriptor getDefinition(String nameOrId, boolean failIfNotFound) {
        if (nameOrId == null) {
            if (failIfNotFound) { throw new FcliSimpleException("No attribute found for name or id: null"); }
            return null;
        }
        var definitions = getAllDefinitions();
        FoDAttributeDefinitionDescriptor found;
        try {
            int id = Integer.parseInt(nameOrId);
            found = definitions.stream().filter(d -> Objects.equals(d.getId(), id)).findFirst().orElse(null);
        } catch (NumberFormatException nfe) {
            String trimmed = nameOrId.trim();
            found = definitions.stream().filter(d -> trimmed.equalsIgnoreCase(d.getName())).findFirst().orElse(null);
        }
        if (found == null && failIfNotFound) {
            throw new FcliSimpleException("No attribute found for name or id: " + nameOrId);
        }
        return found;
    }

    /**
     * Returns a map of required-attribute names to their default values, filtered to the given type.
     */
    public Map<String, String> getRequiredDefaultValues(FoDEnums.AttributeTypes attrType) {
        Map<String, String> result = new HashMap<>();
        for (var def : getAllDefinitions()) {
            if (def.getIsRequired() && (attrType.getValue() == 0 || Objects.equals(def.getAttributeTypeId(), attrType.getValue()))) {
                var defaultValue = getDefaultValue(def);
                if (defaultValue != null) { result.put(def.getName(), defaultValue); }
            }
        }
        return result;
    }

    /**
     * Builds an attribute ArrayNode for create operations. Resolves names to ids, filters by
     * attrType, and optionally adds defaults for any required attributes not already specified.
     */
    public JsonNode buildAttributesNode(FoDEnums.AttributeTypes attrType, Map<String, String> attributesMap, boolean autoReqdAttributes) {
        var effectiveMap = buildEffectiveAttributeUpdates(attrType, null, attributesMap, autoReqdAttributes);
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        for (var entry : effectiveMap.entrySet()) {
            var def = getDefinition(entry.getKey(), true);
            if (attrType.getValue() == 0 || Objects.equals(def.getAttributeTypeId(), attrType.getValue())) {
                ObjectNode attrObj = JsonHelper.getObjectMapper().createObjectNode();
                attrObj.put("id", def.getId());
                attrObj.put("value", entry.getValue());
                attrArray.add(attrObj);
            } else {
                LOG.debug("Skipping attribute '{}' as it is not a {} attribute", def.getName(), attrType);
            }
        }
        return attrArray;
    }

    /**
     * Builds an attribute ArrayNode for update operations. Merges current entity attribute values
     * with user-supplied updates, optionally filling in defaults for required attributes.
     */
    public JsonNode buildAttributesNodeForUpdate(FoDEnums.AttributeTypes attrType,
            ArrayList<FoDAttributeValueDescriptor> currentAttributes, Map<String, String> userSuppliedUpdates,
            boolean autoReqdAttributes) {
        var effectiveUpdates = buildEffectiveAttributeUpdates(attrType, currentAttributes, userSuppliedUpdates, autoReqdAttributes);
        return effectiveUpdates.isEmpty()
                ? attributeValuesToNode(currentAttributes)
                : mergeAttributesNode(currentAttributes, effectiveUpdates);
    }

    /**
     * Merges current entity attribute values with a user-supplied name→value map. Resolves attribute
     * names to IDs. Attributes already on the entity are updated; new attributes are appended.
     */
    public JsonNode mergeAttributesNode(ArrayList<FoDAttributeValueDescriptor> current, Map<String, String> updates) {
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        if (updates == null || updates.isEmpty()) { return attrArray; }

        Map<Integer, String> updatesWithId = new HashMap<>();
        for (var entry : updates.entrySet()) {
            var def = getDefinition(entry.getKey(), true);
            updatesWithId.put(def.getId(), entry.getValue());
        }

        Set<Integer> processedIds = new HashSet<>();
        if (current != null) {
            for (var attr : current) {
                int id = attr.getId();
                ObjectNode attrObj = JsonHelper.getObjectMapper().createObjectNode();
                attrObj.put("id", id);
                attrObj.put("value", updatesWithId.getOrDefault(id, attr.getValue()));
                attrArray.add(attrObj);
                processedIds.add(id);
            }
        }
        for (var entry : updatesWithId.entrySet()) {
            if (!processedIds.contains(entry.getKey())) {
                ObjectNode attrObj = JsonHelper.getObjectMapper().createObjectNode();
                attrObj.put("id", entry.getKey());
                attrObj.put("value", entry.getValue());
                attrArray.add(attrObj);
            }
        }
        return attrArray;
    }

    /**
     * Pure serialization helper: converts a list of entity attribute values to an ArrayNode of
     * {id, value} objects without any network calls. Useful when no updates are needed.
     */
    public static JsonNode attributeValuesToNode(ArrayList<FoDAttributeValueDescriptor> values) {
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        if (values == null || values.isEmpty()) { return attrArray; }
        for (var attr : values) {
            ObjectNode attrObj = JsonHelper.getObjectMapper().createObjectNode();
            attrObj.put("id", attr.getId());
            attrObj.put("value", attr.getValue());
            attrArray.add(attrObj);
        }
        return attrArray;
    }

    /**
     * Creates a new attribute definition. Returns the freshly fetched definition after creation.
     */
    public FoDAttributeDefinitionDescriptor createDefinition(FoDAttributeCreateRequest request) {
        var response = unirest.post(FoDUrls.ATTRIBUTES)
                .headerReplace(HttpHeader.CONTENT_TYPE, "application/json")
                .body(request)
                .asObject(JsonNode.class)
                .getBody();
        if (response.has("success") && response.get("success").asBoolean()) {
            if (!response.has("attributeId")) {
                throw new FcliSimpleException("Response missing attributeId: " + response.toString());
            }
            return fetchFromApi(response.get("attributeId").asText(), true);
        } else {
            throw new FcliSimpleException("Failed to create attribute: " + response.toString());
        }
    }

    /**
     * Updates an existing attribute definition. Returns the freshly fetched definition after update.
     */
    public FoDAttributeDefinitionDescriptor updateDefinition(String attributeId, FoDAttributeUpdateRequest request) {
        var response = unirest.put(FoDUrls.ATTRIBUTE)
                .routeParam("attributeId", attributeId)
                .body(request)
                .asObject(JsonNode.class)
                .getBody();
        if (response.has("success") && response.get("success").asBoolean()) {
            return fetchFromApi(attributeId, true);
        } else {
            throw new FcliSimpleException("Failed to update attribute: " + response.toString());
        }
    }

    /**
     * Fetches a single attribute definition directly from the API, bypassing the lazy-loaded cache.
     * Used after create/update operations where the cached list may be stale.
     */
    private FoDAttributeDefinitionDescriptor fetchFromApi(String nameOrId, boolean failIfNotFound) {
        var request = unirest.get(FoDUrls.ATTRIBUTES);
        JsonNode result;
        try {
            int id = Integer.parseInt(nameOrId);
            result = FoDDataHelper.findUnique(request, String.format("id:%d", id));
        } catch (NumberFormatException nfe) {
            result = FoDDataHelper.findUnique(request, String.format("name:%s", nameOrId));
        }
        if (result == null && failIfNotFound) {
            throw new FcliSimpleException("No attribute found for name or id: " + nameOrId);
        }
        return result == null ? null : JsonHelper.treeToValue(result, FoDAttributeDefinitionDescriptor.class);
    }

    private Map<String, String> buildEffectiveAttributeUpdates(FoDEnums.AttributeTypes attrType,
            ArrayList<FoDAttributeValueDescriptor> currentAttributes,
            Map<String, String> userSuppliedUpdates, boolean autoReqdAttributes) {
        var effective = new LinkedHashMap<String, String>();
        if (autoReqdAttributes) {
            Set<String> covered = new HashSet<>();
            if (currentAttributes != null) {
                currentAttributes.stream()
                        .filter(a -> StringUtils.isNotBlank(a.getValue()))
                        .map(FoDAttributeValueDescriptor::getName)
                        .forEach(covered::add);
            }
            if (userSuppliedUpdates != null) { covered.addAll(userSuppliedUpdates.keySet()); }
            getRequiredDefaultValues(attrType).forEach((k, v) -> { if (!covered.contains(k)) effective.put(k, v); });
        }
        if (userSuppliedUpdates != null) { effective.putAll(userSuppliedUpdates); }
        return effective;
    }

    private String getDefaultValue(FoDAttributeDefinitionDescriptor def) {
        if (StringUtils.isNotBlank(def.getDefaultValue())) { return def.getDefaultValue(); }
        return switch (def.getAttributeDataType()) {
            case "Text" -> "autofilled by fcli";
            case "Boolean" -> String.valueOf(false);
            case "User", "Picklist" -> def.getPicklistValues() != null && !def.getPicklistValues().isEmpty()
                ? def.getPicklistValues().get(0).getName() : null;
            default -> null;
        };
    }
}
