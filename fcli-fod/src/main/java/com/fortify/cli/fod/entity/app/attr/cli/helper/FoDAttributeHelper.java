/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.fod.entity.app.attr.cli.helper;

import java.util.*;

import javax.validation.ValidationException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod.entity.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.FoDUrls;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;

public class FoDAttributeHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final FoDAttributeDescriptor getAttributeDescriptor(UnirestInstance unirestInstance, String attrNameOrId, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.ATTRIBUTES);
        try {
            int attrId = Integer.parseInt(attrNameOrId);
            request = request.queryString("filters", String.format("id:%d", attrId));
        } catch (NumberFormatException nfe) {
            request = request.queryString("filters", String.format("name:%s", attrNameOrId));
        }
        JsonNode attr = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && attr.size() == 0) {
            throw new ValidationException("No attribute found for name or id: " + attrNameOrId);
        } else if (attr.size() > 1) {
            throw new ValidationException("Multiple attributes found for name or id: " + attrNameOrId);
        }
        return attr.size() == 0 ? null : JsonHelper.treeToValue(attr.get(0), FoDAttributeDescriptor.class);
    }

    @SneakyThrows
    public static final Map<String, String> getRequiredAttributes(UnirestInstance unirestInstance) {
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
            // currentLookup.getAttributeTypeId() == 1 is "Application" - filter above does not support querying on this yet!
            if (currentLookup.getIsRequired() && currentLookup.getAttributeTypeId() == 1) {
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

    public static JsonNode getAttributesNode(UnirestInstance unirest, Map<String, String> attributes, Boolean autoReqdAttributes) {
        Map<String, String> attributesMap = (Map<String, String>) attributes;
        if (autoReqdAttributes) {
            // find any required attributes
            Map<String, String> reqAttributesMap = getRequiredAttributes(unirest);
            attributesMap = reqAttributesMap;
        }
        ArrayNode attrArray = getObjectMapper().createArrayNode();
        if (attributesMap == null || attributesMap.isEmpty()) return attrArray;
        for (Map.Entry<String, String> attr : attributesMap.entrySet()) {
            ObjectNode attrObj = getObjectMapper().createObjectNode();
            FoDAttributeDescriptor attributeDescriptor = FoDAttributeHelper.getAttributeDescriptor(unirest, attr.getKey(), true);
            attrObj.put("id", attributeDescriptor.getId());
            attrObj.put("value", attr.getValue());
            attrArray.add(attrObj);
        }
        return attrArray;
    }
}
