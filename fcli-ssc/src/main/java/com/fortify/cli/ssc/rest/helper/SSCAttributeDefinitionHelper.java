package com.fortify.cli.ssc.rest.helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.UnirestInstance;

public final class SSCAttributeDefinitionHelper {
	private final Set<String> attrDuplicateLowerNames = new HashSet<>();
	private final Set<String> attrRequiredGuids = new HashSet<>();
	private final Map<String, JsonNode> attrDefsByGuid = new HashMap<>();
	private final Map<String, String> attrGuidsByLowerName = new HashMap<>();
	private final Map<String, String> attrGuidsByLowerGuid = new HashMap<>();
	private final Map<String, SSCAttributeOptionDefinitionHelper> attrOptionsByGuidMap = new HashMap<>();
	
	public SSCAttributeDefinitionHelper(UnirestInstance unirest) {
		JsonNode attributeDefinitions = unirest.get("/api/v1/attributeDefinitions?limit=-1")
					.queryString("fields", "id,guid,name,category,type,required,hidden,hasDefault,options")
					.asObject(JsonNode.class).getBody().get("data");
		attributeDefinitions.forEach(this::processAttributeDefinition);
	}
	
	private void processAttributeDefinition(JsonNode jsonNode) {
		String guid = jsonNode.get("guid").asText();
		String guidLower = guid.toLowerCase();
		String name = jsonNode.get("name").asText();
		String nameLower = name.toLowerCase();
		String category = jsonNode.get("category").asText();
		String categoryLower = category.toLowerCase();
		boolean required = jsonNode.get("required").asBoolean();
		JsonNode options = jsonNode.get("options");
		
		if ( attrGuidsByLowerName.containsKey(nameLower) ) {
			attrDuplicateLowerNames.add(nameLower); // SSC allows for having the same attribute name in different categories 
		}
		if ( required ) {
			attrRequiredGuids.add(guid);
		}
		
		attrDefsByGuid.put(guid, jsonNode);
		attrOptionsByGuidMap.put(guid, new SSCAttributeOptionDefinitionHelper(options));
		attrGuidsByLowerGuid.put(guidLower, guid);
		attrGuidsByLowerName.put(nameLower, guid);
		attrGuidsByLowerName.put(categoryLower+":"+nameLower, guid);
	}
	
	public String getAttributeGuid(String attributeNameOrGuid) {
		String attributeNameOrGuidLower = attributeNameOrGuid.toLowerCase();
		String guid = null;
		if ( attrGuidsByLowerGuid.containsKey(attributeNameOrGuidLower) ) { 
			guid = attrGuidsByLowerGuid.get(attributeNameOrGuidLower); 
		} else if ( attrDuplicateLowerNames.contains(attributeNameOrGuidLower) ) { 
			throw new IllegalArgumentException("Attribute name '"+attributeNameOrGuid+"' is not unique; either use the guid or <category>:<name>"); 
		} else {
			guid = attrGuidsByLowerName.get(attributeNameOrGuidLower);
		}
		if ( guid==null ) {
			throw new IllegalArgumentException("Attribute name or guid '"+attributeNameOrGuid+"' does not exist");
		}
		return guid;
	}
	
	public Integer getAttributeId(String attributeNameOrGuid) {
		return attrDefsByGuid.get(getAttributeGuid(attributeNameOrGuid)).get("id").asInt();
	}
	
	public String getAttributeName(String attributeNameOrGuid) {
		return attrDefsByGuid.get(getAttributeGuid(attributeNameOrGuid)).get("name").asText();
	}
	
	public String getAttributeType(String attributeNameOrGuid) {
		return attrDefsByGuid.get(getAttributeGuid(attributeNameOrGuid)).get("type").asText();
	}
	
	public Set<String> getRequiredAttributeGuids() {
		return Collections.unmodifiableSet(attrRequiredGuids);
	}
	
	public String getOptionGuid(String attributeNameOrGuid, String optionNameOrGuid) {
		return attrOptionsByGuidMap.get(getAttributeGuid(attributeNameOrGuid)).getOptionGuid(optionNameOrGuid);
	}
	
	public String getOptionName(String attributeNameOrGuid, String optionNameOrGuid) {
		return attrOptionsByGuidMap.get(getAttributeGuid(attributeNameOrGuid)).getOptionName(optionNameOrGuid);
	}
	
	private static final class SSCAttributeOptionDefinitionHelper {
		private final Map<String, String> optionGuidsByLowerGuid = new HashMap<>();
		private final Map<String, String> optionGuidsByLowerName = new HashMap<>();
		private final Map<String, JsonNode> optionsByGuid = new HashMap<>();
		public SSCAttributeOptionDefinitionHelper(JsonNode options) {
			if ( options!=null && !options.isEmpty() && options.isArray() ) {
				options.forEach(this::processOptionDefinition);
			}
		}
		
		private void processOptionDefinition(JsonNode jsonNode) {
			String guid = jsonNode.get("guid").asText();
			String guidLower = guid.toLowerCase();
			String name = jsonNode.get("name").asText();
			String nameLower = name.toLowerCase();
			
			optionGuidsByLowerGuid.put(guidLower, guid);
			optionGuidsByLowerName.put(nameLower, guid);
			optionsByGuid.put(guid, jsonNode);
		}
		
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
		
		public String getOptionName(String optionNameOrGuid) {
			return optionsByGuid.get(getOptionGuid(optionNameOrGuid)).get("name").asText();
		}
	}
}
