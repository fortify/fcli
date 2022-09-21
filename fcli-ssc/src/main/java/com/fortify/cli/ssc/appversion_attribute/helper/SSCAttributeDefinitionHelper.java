package com.fortify.cli.ssc.appversion_attribute.helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
public final class SSCAttributeDefinitionHelper {
    private final Set<String> attrDuplicateLowerNames = new HashSet<>();
    private final Set<String> attrRequiredIds = new HashSet<>();
    private final Map<String, JsonNode> attrDefsById = new HashMap<>();
    private final Map<String, String> attrIdsByLowerName = new HashMap<>();
    private final Map<String, String> attrIdsByLowerGuid = new HashMap<>();
    private final Map<String, SSCAttributeOptionDefinitionHelper> attrOptionsByIdMap = new HashMap<>();
    @Getter private final ObjectNode attributeDefinitionsBody; 
    
    /**
     * This constructor calls the SSC attributeDefinitions endpoint to retrieve attribute definition data,
     * then calls the {@link #processAttributeDefinition(JsonNode)} method for each attribute definition.
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
        String id = jsonNode.get("id").asText();
        String guid = jsonNode.get("guid").asText();
        String guidLower = guid.toLowerCase();
        String name = jsonNode.get("name").asText();
        String nameLower = name.toLowerCase();
        String category = jsonNode.get("category").asText();
        String categoryLower = category.toLowerCase();
        boolean required = jsonNode.get("required").asBoolean();
        JsonNode options = jsonNode.get("options");
        
        if ( attrIdsByLowerName.containsKey(nameLower) ) {
            attrDuplicateLowerNames.add(nameLower); // SSC allows for having the same attribute name in different categories 
        }
        if ( required ) {
            attrRequiredIds.add(id);
        }
        
        attrDefsById.put(id, jsonNode);
        attrOptionsByIdMap.put(id, new SSCAttributeOptionDefinitionHelper(options));
        attrIdsByLowerGuid.put(guidLower, id);
        attrIdsByLowerName.put(nameLower, id);
        attrIdsByLowerName.put(categoryLower+":"+nameLower, id);
    }
    
    /**
     * Get the attribute id for the given attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @return attribute id
     */
    public String getAttributeId(String attributeIdOrGuidOrName) {
        String attributeIdOrGuidOrNameLower = attributeIdOrGuidOrName.toLowerCase();
        String id = null;
        if ( attrDefsById.containsKey(attributeIdOrGuidOrNameLower) ) {
            id = attributeIdOrGuidOrNameLower;
        } else if ( attrIdsByLowerGuid.containsKey(attributeIdOrGuidOrNameLower) ) { 
            id = attrIdsByLowerGuid.get(attributeIdOrGuidOrNameLower); 
        } else if ( attrDuplicateLowerNames.contains(attributeIdOrGuidOrNameLower) ) { 
            throw new IllegalArgumentException("Attribute name '"+attributeIdOrGuidOrNameLower+"' is not unique; either use the guid or <category>:<name>"); 
        } else {
            id = attrIdsByLowerName.get(attributeIdOrGuidOrNameLower);
        }
        if ( id==null ) {
            throw new IllegalArgumentException("Attribute id, guid or name '"+attributeIdOrGuidOrName+"' does not exist");
        }
        return id;
    }
    
    /**
     * Get the attribute guid for the given attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @return attribute name
     */
    public String getAttributeGuid(String attributeIdOrGuidOrName) {
        return attrDefsById.get(getAttributeId(attributeIdOrGuidOrName)).get("guid").asText();
    }
    
    /**
     * Get the attribute name for the given attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @return attribute name
     */
    public String getAttributeName(String attributeIdOrGuidOrName) {
        return attrDefsById.get(getAttributeId(attributeIdOrGuidOrName)).get("name").asText();
    }
    
    /**
     * Get the attribute type for the given attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @return attribute name
     */
    public String getAttributeType(String attributeIdOrGuidOrName) {
        return attrDefsById.get(getAttributeId(attributeIdOrGuidOrName)).get("type").asText();
    }
    
    /**
     * Get a set of required attribute id's
     * @param attributeIdOrGuidOrName
     * @return attribute name
     */
    public Set<String> getRequiredAttributeIds() {
        return Collections.unmodifiableSet(attrRequiredIds);
    }
    
    /**
     * Get the option guid for the given option name or guid, for the given 
     * attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @param optionNameOrGuid
     * @return option guid
     */
    public String getOptionGuid(String attributeIdOrGuidOrName, String optionNameOrGuid) {
        return attrOptionsByIdMap.get(getAttributeId(attributeIdOrGuidOrName)).getOptionGuid(optionNameOrGuid);
    }
    
    /**
     * Get the option name for the given option name or guid, for the given 
     * attribute id, guid, or name.
     * @param attributeIdOrGuidOrName
     * @param optionNameOrGuid
     * @return option name
     */
    public String getOptionName(String attributeIdOrGuidOrName, String optionNameOrGuid) {
        return attrOptionsByIdMap.get(getAttributeId(attributeIdOrGuidOrName)).getOptionName(optionNameOrGuid);
    }
    
    /**
     * This class stores option data for a single attribute definition, and provides
     * various methods for retrieving option definition properties based on option 
     * guid or name.
     * @author rsenden
     *
     */
    private static final class SSCAttributeOptionDefinitionHelper {
        private final Map<String, String> optionGuidsByLowerGuid = new HashMap<>();
        private final Map<String, String> optionGuidsByLowerName = new HashMap<>();
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
            String guidLower = guid.toLowerCase();
            String name = jsonNode.get("name").asText();
            String nameLower = name.toLowerCase();
            
            optionGuidsByLowerGuid.put(guidLower, guid);
            optionGuidsByLowerName.put(nameLower, guid);
            optionsByGuid.put(guid, jsonNode);
        }
        
        /**
         * Get the option guid for the given option guid or name.
         * @param optionNameOrGuid
         * @return option name
         */
        public String getOptionGuid(String optionNameOrGuid) {
            String optionNameOrGuidLower = optionNameOrGuid.toLowerCase();
            String guid = null;
            if ( optionGuidsByLowerGuid.containsKey(optionNameOrGuidLower) ) { 
                guid = optionGuidsByLowerGuid.get(optionNameOrGuidLower); 
            } else {
                guid = optionGuidsByLowerName.get(optionNameOrGuidLower);
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
