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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.attribute_definition.helper.SSCAttributeDefinitionDescriptor;
import com.fortify.cli.ssc.attribute_definition.helper.SSCAttributeDefinitionHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public final class SSCAppVersionAttributeUpdateBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final UnirestInstance unirest;
    private final SSCAttributeDefinitionHelper attributeDefinitionHelper;
    private final Map<String,String> attributes = new LinkedHashMap<>();
    private boolean addRequiredAttributes = false;
    private boolean checkRequiredAttributes = false;
    private ArrayNode preparedAttrUpdateData = null;
    
    public SSCAppVersionAttributeUpdateBuilder(UnirestInstance unirest) {
        this(unirest, new SSCAttributeDefinitionHelper(unirest));
    }
    
    public SSCAppVersionAttributeUpdateBuilder(UnirestInstance unirest, SSCAttributeDefinitionHelper attributeDefinitionHelper) {
        this.unirest = unirest;
        this.attributeDefinitionHelper = attributeDefinitionHelper;
    }
    
    public SSCAppVersionAttributeUpdateBuilder add(Map<String,String> attributes) {
        if ( attributes!=null && !attributes.isEmpty() ) {
            this.attributes.putAll(attributes);
        }
        return resetPreparedRequest();
    }
    
    public SSCAppVersionAttributeUpdateBuilder addRequiredAttrs(boolean addRequiredAttrs) {
        this.addRequiredAttributes = addRequiredAttrs;
        return resetPreparedRequest();
    }
    
    public SSCAppVersionAttributeUpdateBuilder checkRequiredAttrs(boolean checkRequiredAttrs) {
        this.checkRequiredAttributes = checkRequiredAttrs;
        return resetPreparedRequest();
    }
    
    public SSCAppVersionAttributeUpdateBuilder prepareAndCheckRequest() {
        if ( this.preparedAttrUpdateData==null ) {
            this.preparedAttrUpdateData = prepareAttrUpdateData();
            if ( checkRequiredAttributes && !addRequiredAttributes ) {
                checkRequiredAttributesPresent(attributeDefinitionHelper, this.preparedAttrUpdateData);
            }
        }
        return this;
    }

    public HttpRequest<?> buildRequest(String applicationVersionId) {
        prepareAndCheckRequest();
        return unirest.put("/api/v1/projectVersions/{id}/attributes")
                .routeParam("id", applicationVersionId).body(this.preparedAttrUpdateData);
    }
    
    public Set<String> getAttributeIds() {
        prepareAndCheckRequest();
        return getAttributeIds(this.preparedAttrUpdateData);
    }

    private ArrayNode prepareAttrUpdateData() {
        return prepareAttributes().entrySet().stream()
                .map(this::createAttrUpdateNode)
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    private Map<String, String> prepareAttributes() {
        Map<String,String> preparedAttributes = new LinkedHashMap<>();
        if ( addRequiredAttributes ) {
            preparedAttributes.putAll(getRequiredAttributesWithDefaultValues(attributeDefinitionHelper));
        }
        preparedAttributes.putAll(this.attributes);
        return preparedAttributes;
    }

    private ObjectNode createAttrUpdateNode(Map.Entry<String, String> attrEntry) {
        return createAttrUpdateNode(attributeDefinitionHelper, attrEntry);
    }
    
    private SSCAppVersionAttributeUpdateBuilder resetPreparedRequest() {
        this.preparedAttrUpdateData = null;
        return this;
    }
    
    private static final void checkRequiredAttributesPresent(SSCAttributeDefinitionHelper helper, ArrayNode preparedAttributes) {
        Set<String> missingRequiredAttributes = getMissingRequiredAttributes(helper, preparedAttributes);
        if ( !missingRequiredAttributes.isEmpty() ) {
            throw new IllegalArgumentException("The following required attributes must be provided: "+missingRequiredAttributes);
        }
    }
    
    private static final Set<String> getMissingRequiredAttributes(SSCAttributeDefinitionHelper helper, ArrayNode preparedAttributes) {
        Set<String> attributeIds = getAttributeIds(preparedAttributes);
        return helper.getRequiredAttributeWithoutDefaultValueDescriptors()
                .stream()
                .filter(d->!attributeIds.contains(d.getId()))
                .map(SSCAttributeDefinitionDescriptor::getFullName)
                .collect(Collectors.toSet());
    }
    
    private static final Set<String> getAttributeIds(ArrayNode preparedAttributes) {
        return JsonHelper.stream(preparedAttributes)
                .map(j->j.get("attributeDefinitionId"))
                .map(JsonNode::asText)
                .collect(Collectors.toSet());
    }
    
    private static final Map<String, String> getRequiredAttributesWithDefaultValues(SSCAttributeDefinitionHelper helper) {
        return helper.getRequiredAttributeWithoutDefaultValueDescriptors().stream()
                .collect(Collectors.toMap(SSCAttributeDefinitionDescriptor::getId, SSCAppVersionAttributeUpdateBuilder::getDefaultAttributeValue));
    }
    
    private static final String getDefaultAttributeValue(SSCAttributeDefinitionDescriptor descriptor) {
        JsonNode options = descriptor.getOptions();
        if ( options != null && !options.isEmpty() ) {
            return options.get(0).get("guid").asText();
        } else {
            switch ( descriptor.getType() ) {
                case "INTEGER": return null;
                case "BOOLEAN": return "true";
                case "DATE": return new Date().toString();
                default: return "fcli auto-value";
            }
        }
    }
    
    private static final ObjectNode createAttrUpdateNode(SSCAttributeDefinitionHelper helper, Map.Entry<String, String> attrEntry) {
        String attrNameOrId = attrEntry.getKey();
        SSCAttributeDefinitionDescriptor descriptor = helper.getAttributeDefinitionDescriptor(attrNameOrId);
        String attrId = descriptor.getId();
        String type = descriptor.getType();
        String value = attrEntry.getValue();
        
        ObjectNode attrUpdateNode = objectMapper.createObjectNode();
        attrUpdateNode.put("attributeDefinitionId", attrId);
        
        switch ( type ) {
        case "MULTIPLE": 
            attrUpdateNode.set("values", getOptionMultiValues(helper, descriptor, value)); break;
        case "SINGLE":   
            attrUpdateNode.set("values", getOptionSingleValue(helper, descriptor, value)); break;
        case "TEXT":     
            attrUpdateNode.put("value", value); break;
        case "LONG_TEXT":
            attrUpdateNode.put("value", value); break;
        case "SENSITIVE_TEXT":
            attrUpdateNode.put("value", value); break;
        case "BOOLEAN":
            attrUpdateNode.put("value", getOptionBooleanValue(descriptor, value)); break;
        case "INTEGER":
            attrUpdateNode.put("value", getOptionIntegerValue(descriptor, value)); break;
        case "DATE":
            attrUpdateNode.put("value", getOptionDateValue(descriptor, value)); break;
        default:
            throw new IllegalStateException("Unknown attribute type "+type+" for attribute "+descriptor.getFullName());
        }
        return attrUpdateNode;
    }

    private static String getOptionDateValue(SSCAttributeDefinitionDescriptor descriptor, String value) {
        if ( !Pattern.matches("\\d{4}-\\d{2}-\\d{2}", value) ) {
            throw new IllegalArgumentException("Value for attribute '"+descriptor.getFullName()+"' must be specified as yyyy-MM-dd");
        }
        return value;
    }

    private static int getOptionIntegerValue(SSCAttributeDefinitionDescriptor descriptor, String value) {
        try {
            return Integer.parseInt(value);
        } catch ( NumberFormatException nfe ) {
            throw new IllegalArgumentException("Value for attribute '"+descriptor.getFullName()+"' must be an integer");
        }
    }

    private static boolean getOptionBooleanValue(SSCAttributeDefinitionDescriptor descriptor, String value) {
        if ( "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) ) {
            return Boolean.parseBoolean(value);
        } else {
            throw new IllegalArgumentException("Value for attribute '"+descriptor.getFullName()+"' must be 'true' or 'false'");
        }
    }
    
    private static final ArrayNode getOptionMultiValues(SSCAttributeDefinitionHelper helper, SSCAttributeDefinitionDescriptor descriptor, String value) {
        return getOptionValues(helper, descriptor, value);
    }
    
    private static final ArrayNode getOptionSingleValue(SSCAttributeDefinitionHelper helper, SSCAttributeDefinitionDescriptor descriptor, String value) {
        ArrayNode optionValues = getOptionValues(helper, descriptor, value);
        if ( optionValues.size()>1 ) {
            throw new IllegalArgumentException("Attribute '"+descriptor.getFullName()+"' can only contain a single value");
        }
        return optionValues;
    }

    private static ArrayNode getOptionValues(SSCAttributeDefinitionHelper helper, SSCAttributeDefinitionDescriptor descriptor, String value) {
        return Stream.of(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(v->helper.getOptionGuid(descriptor.getGuid(), v))
                .map(SSCAppVersionAttributeUpdateBuilder::createAttrValueNode)
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    private static final ObjectNode createAttrValueNode(String optionGuid) {
        return objectMapper.createObjectNode().put("guid", optionGuid);
    }
}
