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
package com.fortify.cli.ssc.custom_tag.helper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;

public class SSCCustomTagDefinitionHelper {
    private final UnirestInstance unirest;
    @Getter(lazy = true) private final List<SSCCustomTagDescriptor> descriptors = loadDescriptors();

    public SSCCustomTagDefinitionHelper(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    public static final List<SSCCustomTagDescriptor> toDescriptors(JsonNode tagsNode) {
        if ( tagsNode!=null && tagsNode instanceof ObjectNode ) {
            tagsNode = tagsNode.get("data");
        }
        if ( tagsNode==null || !(tagsNode instanceof ArrayNode)) {
            throw new FcliTechnicalException("Invalid custom tags data: "+tagsNode);
        }
        return JsonHelper.stream((ArrayNode)tagsNode)
                .map(tag -> JsonHelper.treeToValue(tag, SSCCustomTagDescriptor.class))
                .toList();
    }

    private List<SSCCustomTagDescriptor> loadDescriptors() {
        return toDescriptors(
                unirest.get(SSCUrls.CUSTOM_TAGS).queryString("limit", "-1").asObject(JsonNode.class).getBody()
        );
    }

    public SSCCustomTagDescriptor getDescriptorByCustomTagSpec(String customTagSpec, boolean failIfNotFound) {
        if (customTagSpec == null || customTagSpec.isBlank()) {
            if (failIfNotFound) {
                throw new FcliSimpleException("Custom tag not found: null or empty");
            }
            return null;
        }
        return getDescriptors().stream()
                .filter(desc -> customTagSpec.equalsIgnoreCase(desc.getGuid())
                        || customTagSpec.equalsIgnoreCase(desc.getName())
                        || customTagSpec.equalsIgnoreCase(desc.getId()))
                .findFirst()
                .orElseGet(() -> {
                    if (failIfNotFound) {
                        throw new FcliSimpleException("Custom tag not found: " + customTagSpec);
                    }
                    return null;
                });
    }

    public Stream<SSCCustomTagDescriptor> getDescriptorsByCustomTagSpec(List<String> customTagSpecs, boolean failIfNotFound) {
        if (customTagSpecs == null || customTagSpecs.isEmpty()) {
            return Stream.empty();
        }
        return customTagSpecs.stream()
                .map(spec -> getDescriptorByCustomTagSpec(spec, failIfNotFound))
                .filter(Objects::nonNull)
                .distinct();
    }

        public ObjectNode buildCreateBody(String name, SSCCustomTagValueType valueType, String description,
            boolean restriction, boolean hidden, boolean requiresComment, boolean extensible, String values) {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("name", name);
        body.put("description", description != null ? description : "");
        body.put("valueType", valueType.name());
        body.put("restriction", restriction);
        body.put("hidden", hidden);
        body.put("requiresComment", requiresComment);
        body.put("customTagType", "CUSTOM");
        if (valueType == SSCCustomTagValueType.LIST) {
            body.put("extensible", extensible);
            body.set("valueList", buildCreateValueList(values));
        }
        return body;
    }

    public ObjectNode buildUpdateBody(SSCCustomTagDescriptor desc, String name, String description,
            Boolean restriction, Boolean hidden, Boolean requiresComment, Boolean extensible,
            String values, String addValues, String rmValues) {
        ObjectNode body = (ObjectNode)desc.asJsonNode().deepCopy();
        if (name != null && !name.isBlank()) { body.put("name", name); }
        if (description != null) { body.put("description", description); }
        if (restriction != null) { body.put("restriction", restriction); }
        if (hidden != null) { body.put("hidden", hidden); }
        if (requiresComment != null) { body.put("requiresComment", requiresComment); }
        
        String valueType = body.path("valueType").asText();
        boolean hasValueModifications = values != null || addValues != null || rmValues != null;
        
        if (hasValueModifications && !"LIST".equalsIgnoreCase(valueType)) {
            throw new FcliSimpleException("Cannot modify values for custom tag '" + desc.getName() + 
                    "': value operations (--values, --add-values, --rm-values) only apply to LIST type tags, not " + valueType);
        }
        
        if ("LIST".equalsIgnoreCase(valueType)) {
            if (extensible != null) {
                body.put("extensible", extensible);
            }
            body.set("valueList", buildUpdatedValueList(body, values, addValues, rmValues));
        }
        return body;
    }

    public int addValueToListTag(String tagGuid, String newValue) {
        SSCCustomTagDescriptor desc = getDescriptorByCustomTagSpec(tagGuid, true);
        return addValueToListTagInternal(desc.getId(), (ObjectNode) desc.asJsonNode().deepCopy(), newValue);
    }

    public int addValueToListTagById(String tagNumericId, String newValue) {
        JsonNode response = unirest.get(SSCUrls.CUSTOM_TAG(tagNumericId)).asObject(JsonNode.class).getBody();
        ObjectNode body = (ObjectNode) response.get("data").deepCopy();
        return addValueToListTagInternal(tagNumericId, body, newValue);
    }

    private int addValueToListTagInternal(String tagNumericId, ObjectNode body, String newValue) {
        LinkedHashMap<String, ObjectNode> valueMap = buildValueMap(body);
        if (valueMap.keySet().stream().noneMatch(k -> k.equalsIgnoreCase(newValue))) {
            valueMap.put(newValue, newValueListEntry(newValue));
        }
        int newLookupIndex = -1;
        ArrayNode newValueList = JsonNodeFactory.instance.arrayNode();
        int idx = 1;
        for (Map.Entry<String, ObjectNode> e : valueMap.entrySet()) {
            e.getValue().put("lookupIndex", idx);
            e.getValue().put("seqNumber", idx);
            newValueList.add(e.getValue());
            if (e.getKey().equalsIgnoreCase(newValue)) {
                newLookupIndex = idx;
            }
            idx++;
        }
        body.set("valueList", newValueList);
        unirest.put(SSCUrls.CUSTOM_TAG(tagNumericId))
                .body(body)
                .asObject(JsonNode.class)
                .getBody();
        return newLookupIndex;
    }

    private ArrayNode buildCreateValueList(String values) {
        if (values == null || values.isBlank()) {
            throw new FcliSimpleException("At least one value must be specified for LIST type using --values");
        }
        var valueList = JsonNodeFactory.instance.arrayNode();
        String[] vals = values.split(",");
        int idx = 1;
        for (int i = 0; i < vals.length; i++) {
            String val = vals[i].trim();
            if (val.isBlank()) {
                continue;
            }
            ObjectNode entry = newValueListEntry(val);
            entry.put("lookupIndex", idx);
            entry.put("seqNumber", idx);
            valueList.add(entry);
            idx++;
        }
        if (valueList.isEmpty()) {
            throw new FcliSimpleException("At least one non-blank value must be specified for LIST type using --values");
        }
        return valueList;
    }

    private ArrayNode buildUpdatedValueList(ObjectNode body, String values, String addValues, String rmValues) {
        LinkedHashMap<String, ObjectNode> valueMap = buildValueMap(body);
        if (values != null) {
            valueMap.clear();
            addValuesToMap(valueMap, values);
        }
        if (addValues != null) {
            addValuesToMap(valueMap, addValues);
        }
        if (rmValues != null) {
            removeValuesFromMap(valueMap, rmValues);
        }
        if (valueMap.isEmpty()) {
            throw new FcliSimpleException("At least one value must remain after update; cannot remove all LIST values");
        }
        var newValueList = JsonNodeFactory.instance.arrayNode();
        int idx = 1;
        for (ObjectNode entry : valueMap.values()) {
            entry.put("lookupIndex", idx);
            entry.put("seqNumber", idx);
            newValueList.add(entry);
            idx++;
        }
        return newValueList;
    }

    private LinkedHashMap<String, ObjectNode> buildValueMap(ObjectNode body) {
        var valueList = body.withArray("valueList");
        LinkedHashMap<String, ObjectNode> valueMap = new LinkedHashMap<>();
        for (JsonNode v : valueList) {
            String key = v.path("lookupValue").asText();
            valueMap.put(key, (ObjectNode)v);
        }
        return valueMap;
    }

    private void addValuesToMap(LinkedHashMap<String, ObjectNode> valueMap, String valuesStr) {
        String[] vals = valuesStr.split(",");
        for (String val : vals) {
            String trimmed = val.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (valueMap.keySet().stream().noneMatch(k -> k.equalsIgnoreCase(trimmed))) {
                valueMap.put(trimmed, newValueListEntry(trimmed));
            }
        }
    }

    private void removeValuesFromMap(LinkedHashMap<String, ObjectNode> valueMap, String valuesStr) {
        String[] vals = valuesStr.split(",");
        for (String val : vals) {
            String trimmed = val.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            valueMap.keySet().removeIf(k -> k.equalsIgnoreCase(trimmed));
        }
    }

    private ObjectNode newValueListEntry(String value) {
        ObjectNode entry = JsonNodeFactory.instance.objectNode();
        entry.put("lookupValue", value);
        entry.put("deletable", true);
        entry.put("description", "");
        entry.putNull("auditAssistantTrainingLabel");
        entry.put("hidden", false);
        return entry;
    }
}