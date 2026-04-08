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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;

public class SSCCustomTagUpdateHelper {
    private final UnirestInstance unirest;
    private final SSCCustomTagHelper tagHelper;

    public SSCCustomTagUpdateHelper(UnirestInstance unirest) {
        this.unirest = unirest;
        this.tagHelper = new SSCCustomTagHelper(unirest);
    }

    public int addValueToListTag(String tagGuid, String newValue) {
        SSCCustomTagDescriptor desc = tagHelper.getDescriptorByCustomTagSpec(tagGuid, true);
        ObjectNode body = (ObjectNode) desc.asJsonNode().deepCopy();
        LinkedHashMap<String, ObjectNode> valueMap = buildValueMap(body);
        if (!valueMap.containsKey(newValue)) {
            ObjectNode entry = JsonNodeFactory.instance.objectNode();
            entry.put("lookupValue", newValue);
            entry.put("deletable", true);
            entry.put("description", "");
            entry.putNull("auditAssistantTrainingLabel");
            entry.put("hidden", false);
            valueMap.put(newValue, entry);
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
        unirest.put(SSCUrls.CUSTOM_TAG(desc.getId()))
                .body(body)
                .asObject(JsonNode.class)
                .getBody();
        return newLookupIndex;
    }

    private LinkedHashMap<String, ObjectNode> buildValueMap(ObjectNode body) {
        var valueList = body.withArray("valueList");
        LinkedHashMap<String, ObjectNode> valueMap = new LinkedHashMap<>();
        for (JsonNode v : valueList) {
            valueMap.put(v.path("lookupValue").asText(), (ObjectNode) v);
        }
        return valueMap;
    }

    /**
     * Resolves tag specs (name, guid, id) to descriptors using SSCCustomTagHelper.
     */
    public Set<SSCCustomTagDescriptor> resolveTagSpecs(List<String> tagSpecs) {
        return tagHelper.getDescriptorsByCustomTagSpec(tagSpecs, false).collect(Collectors.toSet());
    }

    /**
     * Computes the updated stream of custom tag descriptors given current, add, and remove specs.
     */
    public Stream<SSCCustomTagDescriptor> computeUpdatedTagDescriptors(List<SSCCustomTagDescriptor> currentTags, List<String> addSpecs, List<String> rmSpecs) {
        var currentTagsStream = currentTags.stream();
        var addDescriptorsStream = tagHelper.getDescriptorsByCustomTagSpec(addSpecs, false);
        var rmDescriptors = tagHelper.getDescriptorsByCustomTagSpec(rmSpecs, false).toList();
        return Stream.concat(
                currentTagsStream.filter(tag -> rmDescriptors.stream().noneMatch(rmTag -> rmTag.isEqualById(tag))),
                addDescriptorsStream
        ).distinct();
    }

    /**
     * Overload: Accepts current custom tags as json nodes, resolves to descriptors, then computes updated descriptors.
     */
    public Stream<SSCCustomTagDescriptor> computeUpdatedTagDescriptors(JsonNode currentTagsNode, List<String> addSpecs, List<String> rmSpecs) {
        return computeUpdatedTagDescriptors(
                SSCCustomTagHelper.toDescriptors(currentTagsNode),
                addSpecs, rmSpecs);
    }
}