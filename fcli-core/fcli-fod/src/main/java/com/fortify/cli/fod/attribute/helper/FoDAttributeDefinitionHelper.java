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

public class FoDAttributeDefinitionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FoDAttributeDefinitionHelper.class);
    private final UnirestInstance unirest;
    @Getter(lazy = true) private final List<FoDAttributeDefinitionDescriptor> allDefinitions = loadAllDefinitions();

    public FoDAttributeDefinitionHelper(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    private List<FoDAttributeDefinitionDescriptor> loadAllDefinitions() {
        var result = new ArrayList<FoDAttributeDefinitionDescriptor>();
        var body = unirest.get(FoDUrls.ATTRIBUTES).asObject(ObjectNode.class).getBody();
        var items = body.get("items");
        if (items != null && items.isArray()) {
            for (var item : items) {
                var desc = JsonHelper.treeToValue(item, FoDAttributeDefinitionDescriptor.class);
                if (desc != null) { result.add(desc); }
            }
        }
        return result;
    }

    public FoDAttributeDefinitionDescriptor getDefinition(String nameOrId, boolean failIfNotFound) {
        if (nameOrId == null) {
            if (failIfNotFound) throw new FcliSimpleException("No attribute found for name or id: null");
            return null;
        }
        FoDAttributeDefinitionDescriptor result = null;
        try {
            int id = Integer.parseInt(nameOrId);
            result = getAllDefinitions().stream()
                    .filter(d -> d.getId() != null && d.getId() == id)
                    .findFirst().orElse(null);
        } catch (NumberFormatException nfe) {
            String trimmed = nameOrId.trim();
            result = getAllDefinitions().stream()
                    .filter(d -> d.getName() != null && d.getName().trim().equalsIgnoreCase(trimmed))
                    .findFirst().orElse(null);
        }
        if (result == null && failIfNotFound) {
            throw new FcliSimpleException("No attribute found for name or id: " + nameOrId);
        }
        return result;
    }

    public Map<String, String> getRequiredDefaultValues(FoDEnums.AttributeTypes attrType) {
        Map<String, String> reqAttrs = new HashMap<>();
        for (var def : getAllDefinitions()) {
            if (Boolean.TRUE.equals(def.getIsRequired())
                    && (attrType.getValue() == 0 || def.getAttributeTypeId() == attrType.getValue())) {
                var defaultValue = getDefaultValue(def);
                if (defaultValue != null) {
                    reqAttrs.put(def.getName(), defaultValue);
                }
            }
        }
        return reqAttrs;
    }

    public JsonNode buildAttributesNode(FoDEnums.AttributeTypes attrType, Map<String, String> attributesMap,
            boolean autoReqdAttributes) {
        var effectiveMap = buildEffectiveAttributeUpdates(attrType, null, attributesMap, autoReqdAttributes);
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        for (Map.Entry<String, String> attr : effectiveMap.entrySet()) {
            var def = getDefinition(attr.getKey(), true);
            if (Objects.equals(attrType.getValue(), 0) || Objects.equals(def.getAttributeTypeId(), attrType.getValue())) {
                ObjectNode attrObj = JsonHelper.getObjectMapper().createObjectNode();
                attrObj.put("id", def.getId());
                attrObj.put("value", attr.getValue());
                attrArray.add(attrObj);
            } else {
                LOG.debug("Skipping attribute '{}' as it is not a {} attribute", def.getName(), attrType);
            }
        }
        return attrArray;
    }

    public JsonNode buildAttributesNodeForUpdate(FoDEnums.AttributeTypes attrType,
            ArrayList<FoDAttributeValueDescriptor> current, Map<String, String> updates, boolean autoReqd) {
        var effectiveUpdates = buildEffectiveAttributeUpdates(attrType, current, updates, autoReqd);
        return effectiveUpdates.isEmpty()
                ? attributeValuesToNode(current)
                : mergeAttributesNode(current, effectiveUpdates);
    }

    public JsonNode mergeAttributesNode(ArrayList<FoDAttributeValueDescriptor> current, Map<String, String> updates) {
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        if (updates == null || updates.isEmpty()) return attrArray;

        Map<Integer, String> updatesWithId = new HashMap<>();
        for (Map.Entry<String, String> attr : updates.entrySet()) {
            var def = getDefinition(attr.getKey(), true);
            updatesWithId.put(def.getId(), attr.getValue());
        }

        Set<Integer> processedIds = new HashSet<>();
        if (current != null) {
            for (FoDAttributeValueDescriptor attr : current) {
                int id = attr.getId();
                ObjectNode attrObj = JsonHelper.getObjectMapper().createObjectNode();
                attrObj.put("id", id);
                attrObj.put("value", updatesWithId.getOrDefault(id, attr.getValue()));
                attrArray.add(attrObj);
                processedIds.add(id);
            }
        }

        for (Map.Entry<Integer, String> entry : updatesWithId.entrySet()) {
            if (!processedIds.contains(entry.getKey())) {
                ObjectNode attrObj = JsonHelper.getObjectMapper().createObjectNode();
                attrObj.put("id", entry.getKey());
                attrObj.put("value", entry.getValue());
                attrArray.add(attrObj);
            }
        }
        return attrArray;
    }

    public static JsonNode attributeValuesToNode(ArrayList<FoDAttributeValueDescriptor> values) {
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        if (values == null || values.isEmpty()) return attrArray;
        for (FoDAttributeValueDescriptor attr : values) {
            ObjectNode attrObj = JsonHelper.getObjectMapper().createObjectNode();
            attrObj.put("id", attr.getId());
            attrObj.put("value", attr.getValue());
            attrArray.add(attrObj);
        }
        return attrArray;
    }

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

    private FoDAttributeDefinitionDescriptor fetchFromApi(String nameOrId, boolean fail) {
        var request = unirest.get(FoDUrls.ATTRIBUTES);
        JsonNode result;
        try {
            int id = Integer.parseInt(nameOrId);
            result = FoDDataHelper.findUnique(request, String.format("id:%d", id));
        } catch (NumberFormatException nfe) {
            result = FoDDataHelper.findUnique(request, String.format("name:%s", nameOrId));
        }
        if (fail && result == null) {
            throw new FcliSimpleException("No attribute found for name or id: " + nameOrId);
        }
        return result == null ? null : JsonHelper.treeToValue(result, FoDAttributeDefinitionDescriptor.class);
    }

    private Map<String, String> buildEffectiveAttributeUpdates(FoDEnums.AttributeTypes attrType,
            ArrayList<FoDAttributeValueDescriptor> currentAttributes, Map<String, String> userSuppliedUpdates,
            boolean autoReqdAttributes) {
        var effective = new LinkedHashMap<String, String>();
        if (autoReqdAttributes) {
            Set<String> covered = new HashSet<>();
            if (currentAttributes != null) {
                currentAttributes.stream()
                        .filter(a -> StringUtils.isNotBlank(a.getValue()))
                        .map(FoDAttributeValueDescriptor::getName)
                        .forEach(covered::add);
            }
            if (userSuppliedUpdates != null) {
                covered.addAll(userSuppliedUpdates.keySet());
            }
            getRequiredDefaultValues(attrType)
                    .forEach((k, v) -> { if (!covered.contains(k)) effective.put(k, v); });
        }
        if (userSuppliedUpdates != null) {
            effective.putAll(userSuppliedUpdates);
        }
        return effective;
    }

    private String getDefaultValue(FoDAttributeDefinitionDescriptor def) {
        if (StringUtils.isNotBlank(def.getDefaultValue())) {
            return def.getDefaultValue();
        }
        return switch (def.getAttributeDataType()) {
            case "Text" -> "autofilled by fcli";
            case "Boolean" -> String.valueOf(false);
            case "User", "Picklist" -> def.getPicklistValues() != null && !def.getPicklistValues().isEmpty()
                ? def.getPicklistValues().get(0).getName() : null;
            default -> null;
        };
    }
}
