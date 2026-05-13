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
package com.fortify.cli.fod.issue.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDefinitionHelper;

import kong.unirest.UnirestInstance;

public class FoDIssueAttributeHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FoDIssueAttributeHelper.class);
    private final FoDAttributeDefinitionHelper definitionHelper;

    public FoDIssueAttributeHelper(UnirestInstance unirest) {
        this(new FoDAttributeDefinitionHelper(unirest));
    }

    public FoDIssueAttributeHelper(FoDAttributeDefinitionHelper definitionHelper) {
        this.definitionHelper = definitionHelper;
    }

    public ArrayNode buildAttributesNode(Map<String, String> attributeUpdates) {
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        if (attributeUpdates == null || attributeUpdates.isEmpty()) return attrArray;
        for (Map.Entry<String, String> e : attributeUpdates.entrySet()) {
            var def = definitionHelper.getDefinition(e.getKey(), true);
            if (def != null && Objects.equals(def.getAttributeTypeId(), FoDEnums.AttributeTypes.Issue.getValue())) {
                var obj = JsonHelper.getObjectMapper().createObjectNode();
                obj.put("id", def.getId());
                obj.put("value", e.getValue());
                attrArray.add(obj);
            } else if (def != null) {
                LOG.debug("Skipping attribute '{}' as it is not an Issue attribute", def.getName());
            }
        }
        return attrArray;
    }

    public String resolveStatusValue(String value, String[] attrNames, String optionName) {
        FoDEnums.DeveloperStatusType[] devEnum = FoDEnums.DeveloperStatusType.values();
        FoDEnums.AuditorStatusType[] audEnum = FoDEnums.AuditorStatusType.values();
        if (optionName != null && optionName.toLowerCase().contains("developer")) {
            return resolveStatusValue(value, attrNames, optionName, devEnum);
        } else if (optionName != null && optionName.toLowerCase().contains("auditor")) {
            return resolveStatusValue(value, attrNames, optionName, audEnum);
        }
        return resolveStatusValue(value, attrNames, optionName, (FoDEnums.DeveloperStatusType[]) null);
    }

    public <T extends Enum<T> & FoDEnums.IFoDEnumValueSupplier<String>> String resolveStatusValue(
            String value, String[] attrNames, String optionName, T[] enumValues) {
        if (value == null || value.isBlank()) { return null; }
        String originalProvided = value;
        String candidate = value.trim();
        try {
            if (enumValues != null) {
                var resolved = FoDEnums.IFoDEnumValueSupplier.resolveEnumValue(candidate, enumValues);
                if (resolved.isPresent()) { candidate = resolved.get(); }
            }
        } catch (Exception e) {
            LOG.debug("Error resolving enum-style status value for {}: {}", optionName, e.getMessage());
        }

        String attrResolved = tryResolveAgainstAttributes(attrNames, candidate);
        if (attrResolved != null) return attrResolved;

        var allowed = collectAllowedAttributeValues(attrNames);
        throw new FcliSimpleException(String.format("Invalid %s '%s'. Allowed values: %s",
                optionName, originalProvided, String.join(", ", allowed)));
    }

    private String tryResolveAgainstAttributes(String[] attributeNames, String candidate) {
        for (String attrName : attributeNames) {
            var desc = definitionHelper.getDefinition(attrName, false);
            if (desc == null) continue;
            var picklist = desc.getPicklistValues();
            if (picklist == null || picklist.isEmpty()) continue;
            for (var pv : picklist) {
                if (pv.getName() != null && pv.getName().equalsIgnoreCase(candidate)) {
                    return pv.getName();
                }
            }
            try {
                int providedId = Integer.parseInt(candidate);
                for (var pv : picklist) {
                    if (Objects.equals(pv.getId(), providedId)) {
                        return pv.getName();
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private List<String> collectAllowedAttributeValues(String[] attributeNames) {
        var allowed = new ArrayList<String>();
        for (String attrName : attributeNames) {
            var desc = definitionHelper.getDefinition(attrName, false);
            if (desc == null) continue;
            var picklist = desc.getPicklistValues();
            if (picklist == null) continue;
            for (var pv : picklist) {
                allowed.add(pv.getName());
            }
        }
        return allowed;
    }
}
