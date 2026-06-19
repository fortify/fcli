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
package com.fortify.cli.ssc.issue.helper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagDefinitionHelper;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagValueType;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class SSCIssueCustomTagHelper {

    public interface ExtendPolicy {
        boolean canExtend();

        void throwExtendNotAllowedException(String tagName, String validValues);

        record Enabled() implements ExtendPolicy {
            public boolean canExtend() { return true; }
            public void throwExtendNotAllowedException(String tagName, String validValues) {}
        }

        record Disabled(String optionName) implements ExtendPolicy {
            public boolean canExtend() { return false; }
            public void throwExtendNotAllowedException(String tagName, String validValues) {
                String suffix = " Use " + optionName + " to add new values.";
                if (validValues == null || validValues.isBlank()) {
                    throw new FcliSimpleException("Custom tag '" + tagName + "' has no valid list values configured." + suffix);
                }
                throw new FcliSimpleException("Invalid value for custom tag '" + tagName + "'."
                        + " Supported values: " + validValues + "." + suffix);
            }
        }

        static ExtendPolicy enabled() { return new Enabled(); }
        static ExtendPolicy disabled(String optionName) { return new Disabled(optionName); }
    }

    private final UnirestInstance unirest;
    private final String appVersionId;
    
    @Getter(lazy = true) 
    private final Map<String, CustomTagInfo> customTagInfoMap = loadCustomTagInfo();
    
    public List<SSCIssueCustomTagAuditValue> processCustomTags(Map<String,String> customTags, ExtendPolicy extendPolicy) {
        if (customTags == null || customTags.isEmpty()) {
            return List.of();
        }
        
        Map<String, CustomTagInfo> tagInfoMap = getCustomTagInfoMap();
        customTags.forEach((tagName, tagValue) -> validateCustomTagValue(tagName, tagValue, tagInfoMap, extendPolicy));
        
        return customTags.entrySet().stream()
                .map(entry -> {
                    String tagName = entry.getKey();
                    String tagValue = entry.getValue();
                    CustomTagInfo tagInfo = tagInfoMap.get(tagName.toLowerCase());
                    if (tagInfo == null) {
                        throw new FcliSimpleException("Custom tag '" + tagName + "' is not available for this application version");
                    }
                    return createAuditValue(tagName, tagValue, tagInfo, extendPolicy);
                })
                .toList();
    }

    private void validateCustomTagValue(String tagName, String tagValue, Map<String, CustomTagInfo> tagInfoMap, ExtendPolicy extendPolicy) {
        CustomTagInfo tagInfo = tagInfoMap.get(tagName.toLowerCase());
        if (tagInfo == null) {
            throw new FcliSimpleException("Custom tag '" + tagName + "' is not available for this application version");
        }

        boolean isUnset = tagValue == null || tagValue.isBlank();
        if (isUnset) return;
        switch (tagInfo.getValueType()) {
            case TEXT:    return;
            case DECIMAL: validateDecimalValue(tagName, tagValue); return;
            case DATE:    return;
            case LIST:    validateListValue(tagName, tagValue, tagInfo, extendPolicy); return;
            default:
                throw new FcliSimpleException("Unsupported custom tag value type: " + tagInfo.getValueType());
        }
    }

    private void validateDecimalValue(String tagName, String value) {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new FcliSimpleException("Invalid decimal value '" + value + "' for custom tag '" + tagName + "'");
        }
    }

    private void validateListValue(String tagName, String value, CustomTagInfo tagInfo, ExtendPolicy extendPolicy) {
        var valueList = tagInfo.getValueList();
        if (valueList != null) {
            for (ValueListItem item : valueList) {
                if (value.equalsIgnoreCase(item.getLookupValue())) {
                    return;
                }
            }
        }
        if (tagInfo.isExtensible() && extendPolicy.canExtend()) {
            return;
        }
        if (!tagInfo.isExtensible()) {
            String hint = " This tag is not extensible.";
            String validValues = valueList == null || valueList.isEmpty() ? null
                    : valueList.stream().map(ValueListItem::getLookupValue).collect(Collectors.joining(", "));
            if (validValues == null) {
                throw new FcliSimpleException("Custom tag '" + tagName + "' has no valid list values configured." + hint);
            }
            throw new FcliSimpleException("Invalid value '" + value + "' for custom tag '" + tagName + "'."
                    + " Supported values: " + validValues + "." + hint);
        }
        String validValues = valueList == null || valueList.isEmpty() ? null
                : valueList.stream().map(ValueListItem::getLookupValue).collect(Collectors.joining(", "));
        extendPolicy.throwExtendNotAllowedException(tagName, validValues);
    }

    public void populateCustomTagUpdates(Map<String,String> customTags, ArrayNode customTagsArray) {
        if (customTags == null || customTags.isEmpty()) {
            return;
        }
        
        Map<String, CustomTagInfo> tagInfoMap = getCustomTagInfoMap();
        
        customTags.forEach((tagName, tagValue) -> {
            CustomTagInfo tagInfo = tagInfoMap.get(tagName.toLowerCase());
            if (tagInfo == null) {
                throw new FcliSimpleException("Custom tag '" + tagName + "' is not available for this application version");
            }
            
            String displayValue = tagValue == null || tagValue.isBlank() ? "<unset>" : tagValue;
            String valueGuid = getValueGuidForTag(tagValue, tagInfo);
            
            ObjectNode tagNode = JsonHelper.getObjectMapper().createObjectNode();
            tagNode.put("customTagName", tagInfo.getName());
            tagNode.put("customTagGuid", tagInfo.getGuid());
            tagNode.put("value", displayValue);
            if (valueGuid != null) {
                tagNode.put("valueGuid", valueGuid);
            }
            customTagsArray.add(tagNode);
        });
    }
    
    private String getValueGuidForTag(String value, CustomTagInfo tagInfo) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (tagInfo.getValueType() == SSCCustomTagValueType.LIST) {
            if (tagInfo.getValueList() != null) {
                for (ValueListItem item : tagInfo.getValueList()) {
                    if (value.equalsIgnoreCase(item.getLookupValue())) {
                        return String.valueOf(item.getLookupIndex());
                    }
                }
            }
        }
        return null;
    }
    
    private SSCIssueCustomTagAuditValue createAuditValue(String tagName, String value, CustomTagInfo tagInfo, ExtendPolicy extendPolicy) {
        String guid = tagInfo.getGuid();
        boolean isUnset = value == null || value.isBlank();
        switch (tagInfo.getValueType()) {
            case TEXT:
                if (isUnset) return SSCIssueCustomTagAuditValue.forText(guid, "");
                return SSCIssueCustomTagAuditValue.forText(guid, value);
            case DECIMAL:
                if (isUnset) return SSCIssueCustomTagAuditValue.forDecimal(guid, null);
                try {
                    Double decimalValue = Double.parseDouble(value);
                    return SSCIssueCustomTagAuditValue.forDecimal(guid, decimalValue);
                } catch (NumberFormatException e) {
                    throw new FcliSimpleException("Invalid decimal value '" + value + "' for custom tag '" + tagName + "'");
                }
            case DATE:
                if (isUnset) return SSCIssueCustomTagAuditValue.forDate(guid, "");
                String dateValue = processDateValue(value, tagName);
                return SSCIssueCustomTagAuditValue.forDate(guid, dateValue);
            case LIST:
                if (isUnset) return SSCIssueCustomTagAuditValue.forList(guid, -1);
                Integer lookupIndex = getListValueIndex(value, tagName, tagInfo, extendPolicy);
                return SSCIssueCustomTagAuditValue.forList(guid, lookupIndex);
            default:
                throw new FcliSimpleException("Unsupported custom tag value type: " + tagInfo.getValueType());
        }
    }
    
    private void validateDateFormat(String value, String tagName) {
        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new FcliSimpleException("Invalid date format '" + value + "' for custom tag '" + tagName + "'. Expected format: yyyy-MM-dd");
        }
    }

    private String processDateValue(String value, String tagName) {
        validateDateFormat(value, tagName);
        return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    private Integer getListValueIndex(String value, String tagName, CustomTagInfo tagInfo, ExtendPolicy extendPolicy) {
        var valueList = tagInfo.getValueList();
        if (valueList != null) {
            for (ValueListItem item : valueList) {
                if (value.equalsIgnoreCase(item.getLookupValue())) {
                    return item.getLookupIndex();
                }
            }
        }
        if (tagInfo.isExtensible() && extendPolicy.canExtend()) {
            return extendTagWithValue(tagInfo, value);
        }
        if (!tagInfo.isExtensible()) {
            String hint = " This tag is not extensible.";
            String validValues = valueList == null || valueList.isEmpty() ? null
                    : valueList.stream().map(ValueListItem::getLookupValue).collect(Collectors.joining(", "));
            if (validValues == null) {
                throw new FcliSimpleException("Custom tag '" + tagName + "' has no valid list values configured." + hint);
            }
            throw new FcliSimpleException("Invalid value '" + value + "' for custom tag '" + tagName + "'."
                    + " Supported values: " + validValues + "." + hint);
        }
        String validValues = valueList == null || valueList.isEmpty() ? null
                : valueList.stream().map(ValueListItem::getLookupValue).collect(Collectors.joining(", "));
        extendPolicy.throwExtendNotAllowedException(tagName, validValues);
        return -1; // unreachable; throwExtendNotAllowedException always throws
    }
    
    private int extendTagWithValue(CustomTagInfo tagInfo, String newValue) {
        int newIndex = new SSCCustomTagDefinitionHelper(unirest).addValueToListTag(tagInfo.getGuid(), newValue);
        ValueListItem newItem = new ValueListItem();
        newItem.setLookupIndex(newIndex);
        newItem.setLookupValue(newValue);
        tagInfo.getValueList().add(newItem);
        return newIndex;
    }
    
    private Map<String, CustomTagInfo> loadCustomTagInfo() {
        try {
            JsonNode response = unirest.get(SSCUrls.PROJECT_VERSION_CUSTOM_TAGS(appVersionId))
                    .asObject(JsonNode.class)
                    .getBody();
            
            JsonNode dataArray = response.get("data");
            if (dataArray == null || !dataArray.isArray()) {
                throw new FcliSimpleException("Invalid response from custom tags API");
            }
            
            Map<String, CustomTagInfo> result = new HashMap<>();
            
            for (JsonNode tagNode : dataArray) {
                CustomTagInfo tagInfo = parseCustomTagInfo(tagNode);
                result.put(tagInfo.getName().toLowerCase(), tagInfo);
            }
            
            return result;
        } catch (Exception e) {
            if (e instanceof FcliSimpleException fse) throw fse;
            throw new FcliSimpleException("Failed to load custom tag information: " + e.getMessage(), e);
        }
    }
    
    private CustomTagInfo parseCustomTagInfo(JsonNode tagNode) {
        CustomTagInfo tagInfo = new CustomTagInfo();
        tagInfo.setGuid(tagNode.get("guid").asText());
        tagInfo.setName(tagNode.get("name").asText());
        tagInfo.setValueType(SSCCustomTagValueType.valueOf(tagNode.get("valueType").asText()));
        tagInfo.setExtensible(tagNode.path("extensible").asBoolean(false));
        JsonNode valueListNode = tagNode.get("valueList");
        if (valueListNode != null && valueListNode.isArray()) {
            for (JsonNode valueNode : valueListNode) {
                ValueListItem item = new ValueListItem();
                item.setLookupIndex(valueNode.get("lookupIndex").asInt());
                item.setLookupValue(valueNode.get("lookupValue").asText());
                tagInfo.getValueList().add(item);
            }
        }
        
        return tagInfo;
    }
    
    @Getter @Setter
    public static class CustomTagInfo {
        private String guid;
        private String name;
        private SSCCustomTagValueType valueType;
        private boolean extensible;
        private List<ValueListItem> valueList = new ArrayList<>();
    }
    
    @Getter @Setter
    public static class ValueListItem {
        private int lookupIndex;
        private String lookupValue;
    }
}