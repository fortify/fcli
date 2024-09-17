/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.app.attr.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDDataHelper;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;

public class FoDAttributeHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final FoDAttributeDescriptor getAttributeDescriptor(UnirestInstance unirestInstance, String attrNameOrId, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.ATTRIBUTES);
        JsonNode result = null;
        try {
            int attrId = Integer.parseInt(attrNameOrId);
            result = FoDDataHelper.findUnique(request, String.format("id:%d", attrId));
        } catch (NumberFormatException nfe) {
            result = FoDDataHelper.findUnique(request, String.format("name:%s", attrNameOrId));
        }
        if ( failIfNotFound && result==null ) {
            throw new IllegalArgumentException("No attribute found for name or id: " + attrNameOrId);
        }
        return result==null ? null : JsonHelper.treeToValue(result, FoDAttributeDescriptor.class);
    }

    @SneakyThrows
    public static final Map<String, String> getRequiredAttributesDefaultValues(UnirestInstance unirestInstance) {
        Map<String, String> reqAttrs = new HashMap<>();
        GetRequest request = unirestInstance.get(FoDUrls.ATTRIBUTES)
                .queryString("filters", "isRequired:true");
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDAttributeDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDAttributeDescriptor>>() {
                });
        Iterator<FoDAttributeDescriptor> lookupIterator = lookupList.iterator();
        while (lookupIterator.hasNext()) {
            FoDAttributeDescriptor currentLookup = lookupIterator.next();
            // currentLookup.getAttributeTypeId() == 1 is "Application", 4 is "Release" - filter above does not support querying on this yet!
            if (currentLookup.getIsRequired() && (currentLookup.getAttributeTypeId() == 1 || currentLookup.getAttributeTypeId() == 4)) {
                switch (currentLookup.getAttributeDataType()) {
                    case "Text":
                        reqAttrs.put(currentLookup.getName(), "autofilled by fcli");
                        break;
                    case "Boolean":
                        reqAttrs.put(currentLookup.getName(), String.valueOf(false));
                        break;
                    case "User":
                        // use the first user in the list
                        reqAttrs.put(currentLookup.getName(), currentLookup.getPicklistValues().get(0).getName());
                        break;
                    case "Picklist":
                        // use the first value in the picklist
                        reqAttrs.put(currentLookup.getName(), currentLookup.getPicklistValues().get(0).getName());
                        break;
                    default:
                        break;
                }
            }
        }
        return reqAttrs;
    }

    public static JsonNode mergeAttributesNode(UnirestInstance unirest,
                                           ArrayList<FoDAttributeDescriptor> current,
                                           Map<String, String> updates) {
        ArrayNode attrArray = objectMapper.createArrayNode();
        if (updates == null || updates.isEmpty()) return attrArray;
        Map<Integer, String> updatesWithId = new HashMap<>();
        for (Map.Entry<String, String> attr : updates.entrySet()) {
            FoDAttributeDescriptor attributeDescriptor = FoDAttributeHelper.getAttributeDescriptor(unirest, attr.getKey(), true);
            updatesWithId.put(Integer.valueOf(attributeDescriptor.getId()), attr.getValue());
        }
        for (FoDAttributeDescriptor attr : current) {
            ObjectNode attrObj = objectMapper.createObjectNode();
            attrObj.put("id", attr.getId());
            if (updatesWithId.containsKey(Integer.valueOf(attr.getId()))) {
                attrObj.put("value", updatesWithId.get(Integer.valueOf(attr.getId())));
            } else {
                attrObj.put("value", attr.getValue());
            }
            attrArray.add(attrObj);
        }
        return attrArray;
    }

    public static JsonNode getAttributesNode(ArrayList<FoDAttributeDescriptor> attributes) {
        ArrayNode attrArray = objectMapper.createArrayNode();
        if (attributes == null || attributes.isEmpty()) return attrArray;
        for (FoDAttributeDescriptor attr : attributes) {
            ObjectNode attrObj = objectMapper.createObjectNode();
            attrObj.put("id", attr.getId());
            attrObj.put("value", attr.getValue());
            attrArray.add(attrObj);
        }
        return attrArray;
    }

    public static JsonNode getAttributesNode(UnirestInstance unirest, Map<String, String> attributesMap, boolean autoReqdAttributes) {
        Map<String, String> combinedAttributesMap = new LinkedHashMap<>();
        if (autoReqdAttributes) {
            // find any required attributes
            combinedAttributesMap.putAll(getRequiredAttributesDefaultValues(unirest));
        }
        if ( attributesMap!=null && !attributesMap.isEmpty() ) {
            combinedAttributesMap.putAll(attributesMap);
        }
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        for (Map.Entry<String, String> attr : combinedAttributesMap.entrySet()) {
            ObjectNode attrObj = getObjectMapper().createObjectNode();
            FoDAttributeDescriptor attributeDescriptor = FoDAttributeHelper.getAttributeDescriptor(unirest, attr.getKey(), true);
            attrObj.put("id", attributeDescriptor.getId());
            attrObj.put("value", attr.getValue());
            attrArray.add(attrObj);
        }
        return attrArray;
    }
}
