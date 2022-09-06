/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.appversion_attribute.helper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

@ReflectiveAccess @RequiredArgsConstructor
public final class SSCAppVersionAttributeUpdateHelper {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final SSCAttributeDefinitionHelper attributeDefinitionHelper;
    private final Map<String,String> attributes;
    
    public HttpRequest<?> getAttributeUpdateRequest(UnirestInstance unirest, String applicationVersionId) {
        ArrayNode attrUpdateData = objectMapper.createArrayNode().addAll( 
                attributes.entrySet().stream().map(this::createAttrUpdateNode)
                .collect(Collectors.toList()));
        return unirest.put("/api/v1/projectVersions/{id}/attributes")
                .routeParam("id", applicationVersionId).body(attrUpdateData);
    }
    
    public Set<String> getAttributeIds() {
        return attributes.keySet().stream().map(attributeDefinitionHelper::getAttributeId).collect(Collectors.toSet());
    }
    
    private ObjectNode createAttrUpdateNode(Map.Entry<String, String> attrEntry) {
        String attrNameOrId = attrEntry.getKey();
        String attrGuid = attributeDefinitionHelper.getAttributeGuid(attrNameOrId);
        String attrId = attributeDefinitionHelper.getAttributeId(attrGuid);
        String type = attributeDefinitionHelper.getAttributeType(attrGuid);
        List<String> valueGuids = Stream.of(attrEntry.getValue().split(","))
                .filter(Predicate.not(String::isBlank))
                .map(v->attributeDefinitionHelper.getOptionGuid(attrGuid, v))
                .collect(Collectors.toList());
        
        ObjectNode attrUpdateNode = objectMapper.createObjectNode();
        attrUpdateNode.put("attributeDefinitionId", attrId);
        if ( !"MULTIPLE".equals(type) && valueGuids.size()>1 ) {
            throw new IllegalArgumentException("Attribute "+attrNameOrId+" can only contain a single value");
        }
        if ( "MULTIPLE".equals(type) || "SINGLE".equals(type) ) {
            ArrayNode valueNodes = objectMapper.createArrayNode().addAll(
                    valueGuids.stream().map(this::createAttrValueNode).collect(Collectors.toList()));
            attrUpdateNode.set("values", valueNodes);
        } else {
            attrUpdateNode.put("value", valueGuids.get(0));
        }
        return attrUpdateNode;
    }
    
    private ObjectNode createAttrValueNode(String optionGuid) {
        return objectMapper.createObjectNode().put("guid", optionGuid);
    }
}
