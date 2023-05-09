package com.fortify.cli.ssc.entity.appversion_attribute.helper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.entity.attribute_definition.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.rest.helper.SSCInputTransformer;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

/**
 * The SSC /api/v1/projectVersions/{id}/attributes endpoint returns
 * only attributes that have a value set, and returns raw attribute 
 * data with only attribute definition id's and option id's. This 
 * class provides functionality for merging this raw data with data 
 * returned by the /api/v1/attributeDefinitions endpoint to also
 * include attributes that have no value set, and to insert attribute
 * and value names (together with any other applicable definition 
 * properties).
 *  
 * @author rsenden
 *
 */
public class SSCAppVersionAttributeHelper {
    private final ArrayNode cachedAttributeDefinitions;
    private final Set<String> attrIdsToInclude;
    
    public SSCAppVersionAttributeHelper(SSCAttributeDefinitionHelper attrDefHelper, String... attrIdsToInclude) {
        this(attrDefHelper.getAttributeDefinitions(), attrIdsToInclude);
    }
    
    public SSCAppVersionAttributeHelper(JsonNode attrDefs, String... attrIdsToInclude) {
        this(attrDefs, new HashSet<>(Arrays.asList(attrIdsToInclude)));
    }
    
    public SSCAppVersionAttributeHelper(JsonNode attrDefs, Set<String> attrIdsToInclude) {
        this.cachedAttributeDefinitions = (ArrayNode)SSCInputTransformer.getDataOrSelf(attrDefs);
        this.attrIdsToInclude = attrIdsToInclude;
    }
    
    /**
     * Return an {@link HttpRequest} of which the response can be passed to
     * one of the constructors. This is useful for including the request in
     * an SSC bulk request.
     */
    public static final HttpRequest<?> getAttributeDefinitionsRequest(UnirestInstance unirest) {
        return SSCAttributeDefinitionHelper.getAttributeDefinitionsRequest(unirest);
    }
    
    public ArrayNode mergeAttributeDefinitions(JsonNode appVersionAttrs) {
        JsonNode appVersionAttrsData = SSCInputTransformer.getDataOrSelf(appVersionAttrs);
        return JsonHelper.stream(cachedAttributeDefinitions)
                .filter(this::isIncluded)
                .map(attrDef->combine((ObjectNode)attrDef, getAttributeNode(appVersionAttrsData, attrDef)))
                .map(this::addValueString)
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    /**
     * This method takes the output of the {@link #mergeAttributeDefinitions(JsonNode)}
     * method, and returns an {@link ObjectNode} with attribute names as keys, and
     * attribute value(s) as values.
     */
    public ObjectNode getAttributeValues(JsonNode appVersionAttrs, String by) {
        var result = JsonHelper.getObjectMapper().createObjectNode();
        JsonHelper.stream(mergeAttributeDefinitions(appVersionAttrs)).forEach(
                attr->result.set(attr.get(by).asText(), getValues(attr)));
        return result;
    }
    
    private ObjectNode getValues(JsonNode mergedAttr) {
        var result = JsonHelper.getObjectMapper().createObjectNode();
        var value = mergedAttr.get("value");
        var values = JsonHelper.evaluateSpelExpression(mergedAttr, "values?.![name]", ArrayNode.class);
        if ( values!=null && values.size()==1 && (value==null || value.isNull()) ) {
            value = values.get(0);
        }
        if ( value!=null && !value.isNull() && (values==null || values.isEmpty()) ) {
            values = JsonHelper.toArrayNode(value);
        }
        if ( values==null || values.isNull() ) {
            values = JsonHelper.toArrayNode(new String[] {});
        }
        result.set("value", value);
        result.set("values", values);
        result.set("valuesString", mergedAttr.get("valueString"));
        return result;
    }
    
    private ObjectNode combine(ObjectNode node1, ObjectNode node2) {
        ObjectNode result = new ObjectMapper().createObjectNode();
        if ( node1!=null ) { result.setAll(node1); }
        if ( node2!=null ) { result.setAll(node2); }
        return result;
    }
    
    private boolean isIncluded(JsonNode attrDef) {
        return attrIdsToInclude==null || attrIdsToInclude.isEmpty() || attrIdsToInclude.contains(attrDef.get("id").asText());
    }
    
    private ObjectNode getAttributeNode(JsonNode appVersionAttrsData, JsonNode attrDef) {
        String guid = attrDef.get("guid").asText();
        return JsonHelper
                .evaluateSpelExpression(appVersionAttrsData, String.format("#this.^[guid == '%s']", guid), ObjectNode.class);
    }
    
    /**
     * Add the <code>valueString</code> property to the given {@link ObjectNode} 
     * that contains combined attribute and attribute definition data. If the 
     * {@link ObjectNode} contains a <code>value</code> attribute, it's value will
     * be stored in the <code>valueString</code> property. Otherwise, if the 
     * {@link ObjectNode} contains a <code>values</code> property, the value names
     * from this property will be stored as a comma-separated string in the 
     * <code>valueString</code> property.
     * @param attr
     */
    private ObjectNode addValueString(ObjectNode attr) {
        String valueString = "";
        if ( attr.has("value") && !attr.get("value").isNull() ) {
            valueString = attr.get("value").asText();
        } else if ( attr.has("values") && !attr.get("values").isEmpty() ) {
            var values = JsonHelper.evaluateSpelExpression(attr, "values?.![name]", ArrayNode.class);
            valueString = JsonHelper.stream(values).map(JsonNode::asText).collect(Collectors.joining(", "));
        }
        attr.put("valueString", valueString);
        return attr;
    }
}
