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

/**
 * Instance-based helper for FoD issue attribute operations. Delegates definition lookups to
 * {@link FoDAttributeDefinitionHelper} and provides issue-specific attribute node building and
 * status value resolution. Accepts a caller-supplied definition helper to avoid redundant API
 * calls when the caller already holds one, or creates its own from a {@link UnirestInstance}.
 * Intended to be instantiated once per command execution; never stored statically.
 */
public class FoDIssueAttributeHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FoDIssueAttributeHelper.class);
    private final FoDAttributeDefinitionHelper definitionHelper;

    public FoDIssueAttributeHelper(UnirestInstance unirest) {
        this(new FoDAttributeDefinitionHelper(unirest));
    }

    public FoDIssueAttributeHelper(FoDAttributeDefinitionHelper definitionHelper) {
        this.definitionHelper = definitionHelper;
    }

    /**
     * Builds an ArrayNode of {id, value} objects for issue attribute updates, filtering to
     * Issue-scoped attributes only.
     */
    public ArrayNode buildAttributesNode(Map<String, String> attributeUpdates) {
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        if (attributeUpdates == null || attributeUpdates.isEmpty()) { return attrArray; }
        for (var entry : attributeUpdates.entrySet()) {
            var def = definitionHelper.getDefinition(entry.getKey(), false);
            if (def == null) {
                LOG.warn("Attribute '{}' not found, skipping", entry.getKey());
                continue;
            }
            if (Objects.equals(def.getAttributeTypeId(), FoDEnums.AttributeTypes.Issue.getValue())) {
                var obj = JsonHelper.getObjectMapper().createObjectNode();
                obj.put("id", def.getId());
                obj.put("value", entry.getValue());
                attrArray.add(obj);
            } else {
                LOG.debug("Skipping attribute '{}' as it is not an Issue attribute", def.getName());
            }
        }
        return attrArray;
    }

    /**
     * Resolves a developer/auditor status value against attribute picklists, inferring the
     * relevant enum type from the option name.
     */
    public String resolveStatusValue(String providedValue, String[] attributeNames, String optionName) {
        if (optionName != null && optionName.toLowerCase().contains("developer")) {
            return resolveStatusValue(providedValue, attributeNames, optionName, FoDEnums.DeveloperStatusType.values());
        } else if (optionName != null && optionName.toLowerCase().contains("auditor")) {
            return resolveStatusValue(providedValue, attributeNames, optionName, FoDEnums.AuditorStatusType.values());
        }
        return resolveStatusValue(providedValue, attributeNames, optionName, (FoDEnums.DeveloperStatusType[]) null);
    }

    /**
     * Resolves a status value against the given enum values first, then against attribute picklists.
     * Throws a {@link FcliSimpleException} listing allowed values if resolution fails.
     */
    public <T extends Enum<T> & FoDEnums.IFoDEnumValueSupplier<String>> String resolveStatusValue(
            String providedValue, String[] attributeNames, String optionName, T[] enumValues) {
        if (providedValue == null || providedValue.isBlank()) { return null; }
        String originalProvided = providedValue;
        String candidate = providedValue.trim();
        try {
            if (enumValues != null) {
                var resolved = FoDEnums.IFoDEnumValueSupplier.resolveEnumValue(candidate, enumValues);
                if (resolved.isPresent()) { candidate = resolved.get(); }
            }
        } catch (Exception e) {
            LOG.debug("Error resolving enum-style status value for {}: {}", optionName, e.getMessage());
        }

        String attrResolved = tryResolveAgainstAttributes(attributeNames, candidate);
        if (attrResolved != null) { return attrResolved; }

        var allowed = collectAllowedAttributeValues(attributeNames);
        throw new FcliSimpleException(String.format("Invalid %s '%s'. Allowed values: %s",
                optionName, originalProvided, String.join(", ", allowed)));
    }

    private String tryResolveAgainstAttributes(String[] attributeNames, String candidate) {
        for (String attrName : attributeNames) {
            var def = definitionHelper.getDefinition(attrName, false);
            if (def == null) { continue; }
            var picklist = def.getPicklistValues();
            if (picklist == null || picklist.isEmpty()) { continue; }
            for (var pv : picklist) {
                if (pv.getName() != null && pv.getName().equalsIgnoreCase(candidate)) { return pv.getName(); }
            }
            try {
                int providedId = Integer.parseInt(candidate);
                for (var pv : picklist) {
                    if (Objects.equals(pv.getId(), providedId)) { return pv.getName(); }
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private List<String> collectAllowedAttributeValues(String[] attributeNames) {
        var allowed = new ArrayList<String>();
        for (String attrName : attributeNames) {
            var def = definitionHelper.getDefinition(attrName, false);
            if (def == null) { continue; }
            var picklist = def.getPicklistValues();
            if (picklist == null) { continue; }
            for (var pv : picklist) { allowed.add(pv.getName()); }
        }
        return allowed;
    }
}
