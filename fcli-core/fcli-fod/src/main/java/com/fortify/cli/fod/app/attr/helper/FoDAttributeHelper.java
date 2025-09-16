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

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDDataHelper;
import com.fortify.cli.fod._common.util.FoDEnums;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;

public class FoDAttributeHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FoDAttributeHelper.class);
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
            throw new FcliSimpleException("No attribute found for name or id: " + attrNameOrId);
        }
        return result==null ? null : JsonHelper.treeToValue(result, FoDAttributeDescriptor.class);
    }

    @SneakyThrows
    public static final Map<String, String> getRequiredAttributesDefaultValues(UnirestInstance unirestInstance,
        FoDEnums.AttributeTypes attrType) {
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
            // currentLookup.getAttributeTypeId() == 1 if "Application", 4 if "Release" - filter above does not support querying on this yet!
            if (currentLookup.getIsRequired() && (attrType.getValue() == 0 || currentLookup.getAttributeTypeId() == attrType.getValue())) {
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

    public static JsonNode mergeAttributesNode(UnirestInstance unirest, FoDEnums.AttributeTypes attrType,
                                               ArrayList<FoDAttributeDescriptor> current,
                                               Map<String, String> updates) {
        ArrayNode attrArray = objectMapper.createArrayNode();
        if (updates == null || updates.isEmpty()) return attrArray;

        // Map attribute id to value from updates
        Map<Integer, String> updatesWithId = new HashMap<>();
        for (Map.Entry<String, String> attr : updates.entrySet()) {
            FoDAttributeDescriptor desc = getAttributeDescriptor(unirest, attr.getKey(), true);
            updatesWithId.put(desc.getId(), attr.getValue());
        }

        // Track which ids have been processed
        Set<Integer> processedIds = new HashSet<>();

        // Add current attributes, updating values if present in updates
        if (current != null) {
            for (FoDAttributeDescriptor attr : current) {
                int id = attr.getId();
                ObjectNode attrObj = objectMapper.createObjectNode();
                attrObj.put("id", id);
                attrObj.put("value", updatesWithId.getOrDefault(id, attr.getValue()));
                attrArray.add(attrObj);
                processedIds.add(id);
            }
        }

        // Add new attributes from updates not already in current
        for (Map.Entry<Integer, String> entry : updatesWithId.entrySet()) {
            if (!processedIds.contains(entry.getKey())) {
                ObjectNode attrObj = objectMapper.createObjectNode();
                attrObj.put("id", entry.getKey());
                attrObj.put("value", entry.getValue());
                attrArray.add(attrObj);
            }
        }

        return attrArray;
    }

    public static JsonNode getAttributesNode(FoDEnums.AttributeTypes attrType, ArrayList<FoDAttributeDescriptor> attributes) {
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

    public static JsonNode getAttributesNode(UnirestInstance unirest, FoDEnums.AttributeTypes attrType, 
        Map<String, String> attributesMap, boolean autoReqdAttributes) {
        Map<String, String> combinedAttributesMap = new LinkedHashMap<>();
        if (autoReqdAttributes) {
            // find any required attributes
            combinedAttributesMap.putAll(getRequiredAttributesDefaultValues(unirest, attrType));
        }
        if ( attributesMap!=null && !attributesMap.isEmpty() ) {
            combinedAttributesMap.putAll(attributesMap);
        }
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        for (Map.Entry<String, String> attr : combinedAttributesMap.entrySet()) {
            ObjectNode attrObj = getObjectMapper().createObjectNode();
            FoDAttributeDescriptor attributeDescriptor = FoDAttributeHelper.getAttributeDescriptor(unirest, attr.getKey(), true);
            // filter out any attributes that aren't valid for the entity we are working on, e.g. Application or Release
            if (attrType.getValue() == 0 || attributeDescriptor.getAttributeTypeId() == attrType.getValue()) {
                attrObj.put("id", attributeDescriptor.getId());
                attrObj.put("value", attr.getValue());
                attrArray.add(attrObj);
            } else {
                LOG.debug("Skipping attribute '"+attributeDescriptor.getName()+"' as it is not a "+attrType.toString()+" attribute");

            }   
        }
        return attrArray;
    }
}
