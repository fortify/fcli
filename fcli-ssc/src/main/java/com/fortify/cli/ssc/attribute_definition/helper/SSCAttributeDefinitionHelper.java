package com.fortify.cli.ssc.attribute_definition.helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;

/**
 * This class loads SSC attribute definition data, and provides various methods for
 * retrieving attribute definition and option properties based on id, guid, or name,
 * where name may either be a plain attribute name (if the name is unique in SSC),
 * or prefixed with attribute category (as SSC requires names to be unique within a
 * single category).
 *   
 * @author rsenden
 */
// TODO Properly embed option handling/retrieval in SSCAttributeDefinitionDescriptor
public final class SSCAttributeDefinitionHelper {
    private final Set<String> attrDuplicateNames = new HashSet<>();
    private final Set<SSCAttributeDefinitionDescriptor> requiredAttrDefWithoutDefaultValueDescriptors = new HashSet<>();
    private final Map<String, SSCAttributeDefinitionDescriptor> descriptorsById = new HashMap<>();
    private final Map<String, SSCAttributeDefinitionDescriptor> descriptorsByName = new HashMap<>();
    private final Map<String, SSCAttributeDefinitionDescriptor> descriptorsByGuid = new HashMap<>();
    private final Map<String, SSCAttributeOptionDefinitionHelper> attrOptionsByIdMap = new HashMap<>();
    @Getter private final ObjectNode attributeDefinitionsBody; 
    
    /**
     * This constructor calls the SSC attributeDefinitions endpoint to retrieve attribute definition data,
     * then calls the {@link #processAttributeDefinition(JsonNode)} method for each attribute definition
     * to collect the relevant details.
     * @param unirest
     */
    public SSCAttributeDefinitionHelper(UnirestInstance unirest) {
        this.attributeDefinitionsBody = unirest.get("/api/v1/attributeDefinitions?limit=-1&orderby=category,name&fields=id,guid,name,category,type,required,hidden,hasDefault,options")
                    .asObject(ObjectNode.class).getBody();
        
        attributeDefinitionsBody.get("data").forEach(this::processAttributeDefinition);
    }
    
    /**
     * This method stores attribute definition data from the given {@link JsonNode}
     * (representing a single attribute definition) in various instance variables
     * for easy lookup. 
     * @param jsonNode representing a single attribute definition
     */
    private void processAttributeDefinition(JsonNode jsonNode) {
        SSCAttributeDefinitionDescriptor descriptor = JsonHelper.treeToValue(jsonNode, SSCAttributeDefinitionDescriptor.class);
        if ( "DYNAMIC_SCAN_REQUEST".equals(descriptor.getCategory()) ) { return; } // We don't want to process these
        String id = descriptor.getId();
        String guid = descriptor.getGuid();
        String name = descriptor.getName();
        String category = descriptor.getCategory();
        
        if ( descriptorsByName.containsKey(name) ) {
            attrDuplicateNames.add(name); // SSC allows for having the same attribute name in different categories 
        }
        if ( descriptor.isRequired() && !descriptor.hasDefault() ) {
            requiredAttrDefWithoutDefaultValueDescriptors.add(descriptor);
        }
        
        descriptorsById.put(id, descriptor);
        descriptorsByGuid.put(guid, descriptor);
        descriptorsByGuid.put(category+":"+guid, descriptor);
        descriptorsByName.put(name, descriptor);
        descriptorsByName.put(category+":"+name, descriptor);
        attrOptionsByIdMap.put(id, new SSCAttributeOptionDefinitionHelper(descriptor.getOptions()));
    }
    
    /**
     * Get the attribute definition descriptor for the given attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @return {@link SSCAttributeDefinitionDescriptor} instance
     */
    public SSCAttributeDefinitionDescriptor getAttributeDefinitionDescriptor(String attributeIdOrGuidOrName) {
        SSCAttributeDefinitionDescriptor descriptor = descriptorsById.get(attributeIdOrGuidOrName);
        if ( descriptor==null ) { descriptor = descriptorsByGuid.get(attributeIdOrGuidOrName); } 
        if ( descriptor==null && attrDuplicateNames.contains(attributeIdOrGuidOrName) ) { 
            throw new IllegalArgumentException("Attribute name '"+attributeIdOrGuidOrName+"' is not unique; either use the guid or <category>:<name>"); 
        }
        if ( descriptor==null ) { descriptor = descriptorsByName.get(attributeIdOrGuidOrName); }
        if ( descriptor==null ) {
            throw new IllegalArgumentException("Attribute id, guid or name '"+attributeIdOrGuidOrName+"' does not exist");
        }
        return descriptor;
    }
    
    /**
     * @return Set of {@link SSCAttributeDefinitionDescriptor} for attributes that are required 
     */
    public Set<SSCAttributeDefinitionDescriptor> getRequiredAttributeWithoutDefaultValueDescriptors() {
        return Collections.unmodifiableSet(requiredAttrDefWithoutDefaultValueDescriptors);
    }
    
    /**
     * Get the option guid for the given option name or guid, for the given 
     * attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @param optionNameOrGuid
     * @return option guid
     */
    public String getOptionGuid(String attributeIdOrGuidOrName, String optionNameOrGuid) {
        return attrOptionsByIdMap.get(getAttributeDefinitionDescriptor(attributeIdOrGuidOrName).getId()).getOptionGuid(optionNameOrGuid);
    }
    
    /**
     * Get the option name for the given option name or guid, for the given 
     * attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @param optionNameOrGuid
     * @return option name
     */
    public String getOptionName(String attributeIdOrGuidOrName, String optionNameOrGuid) {
        return attrOptionsByIdMap.get(getAttributeDefinitionDescriptor(attributeIdOrGuidOrName).getId()).getOptionName(optionNameOrGuid);
    }
    
    /**
     * This class stores option data for a single attribute definition, and provides
     * various methods for retrieving option definition properties based on option 
     * guid or name.
     * @author rsenden
     *
     */
    private static final class SSCAttributeOptionDefinitionHelper {
        private final Map<String, String> optionGuidsByGuid = new HashMap<>();
        private final Map<String, String> optionGuidsByName = new HashMap<>();
        private final Map<String, JsonNode> optionsByGuid = new HashMap<>();
        
        /**
         * This constructor takes a {@link JsonNode} representing an attribute
         * definition options array, and calls the {@link #processOptionDefinition(JsonNode)}
         * method for each option definition to collect the relevant data. 
         * @param options
         */
        public SSCAttributeOptionDefinitionHelper(JsonNode options) {
            if ( options!=null && !options.isEmpty() && options.isArray() ) {
                options.forEach(this::processOptionDefinition);
            }
        }
        
        /**
         * This method stores option definition data from the given {@link JsonNode}
         * (representing a single option definition) in various instance variables
         * for easy lookup. 
         * @param jsonNode representing a single option definition
         */
        private void processOptionDefinition(JsonNode jsonNode) {
            String guid = jsonNode.get("guid").asText();
            String name = jsonNode.get("name").asText();
            
            optionGuidsByGuid.put(guid, guid);
            optionGuidsByName.put(name, guid);
            optionsByGuid.put(guid, jsonNode);
        }
        
        /**
         * Get the option guid for the given option guid or name.
         * @param optionNameOrGuid
         * @return option name
         */
        public String getOptionGuid(String optionNameOrGuid) {
            String guid = null;
            if ( optionGuidsByGuid.containsKey(optionNameOrGuid) ) { 
                guid = optionGuidsByGuid.get(optionNameOrGuid); 
            } else {
                guid = optionGuidsByName.get(optionNameOrGuid);
            }
            if ( guid==null ) {
                throw new IllegalArgumentException("Option name or guid '"+optionNameOrGuid+"' does not exist");
            }
            return guid;
        }
        
        /**
         * Get the option name for the given option guid or name.
         * @param optionNameOrGuid
         * @return option name
         */
        public String getOptionName(String optionNameOrGuid) {
            return optionsByGuid.get(getOptionGuid(optionNameOrGuid)).get("name").asText();
        }
    }
}
