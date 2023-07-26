/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.rest.lookup.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.rest.lookup.cli.mixin.FoDLookupTypeOptions;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import javax.validation.ValidationException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FoDLookupHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] lookupItems = new String[]{
            "MobileScanPlatformTypes",
            "MobileScanFrameworkTypes",
            "MobileScanEnvironmentTypes",
            "MobileScanRoleTypes",
            "MobileScanExternalDeviceTypes",
            "DynamicScanEnvironmentFacingTypes",
            "DynamicScanAuthenticationTypes",
            "TimeZones",
            "RepeatScheduleTypes",
            "GeoLocations",
            "SDLCStatusTypes",
            "DayOfWeekTypes",
            "BusinessCriticalityTypes",
            "ReportTemplateTypes",
            "AnalysisStatusTypes",
            "ScanStatusTypes",
            "ReportFormats",
            "Roles",
            "ScanPreferenceTypes",
            "AuditPreferenceTypes",
            "EntitlementFrequencyTypes",
            "ApplicationTypes",
            "ScanTypes",
            "AttributeTypes",
            "AttributeDataTypes",
            "MultiFactorAuthorizationTypes",
            "ReportTypes",
            "ReportStatusTypes",
            "PassFailReasonTypes",
            "DynamicScanWebServiceTypes",
            "VulnerabilitySeverityTypes",
            "TechnologyTypes",
            "LanguageLevels",
            "AuditActionTypes",
            "NotificationTriggerTypes",
            "ConcurrentRequestThreadsTypes",
            "MobileScanAuditPreferenceTypes",
            "DataExportTypes",
            "ScanMethodTypes",
            "StartScanMethodTypes",
            "AuditTemplateConditionTypes",
            "OpenSourceScanTypeAuditTemplateFieldTypes",
            "StaticScanTypeAuditTemplateFieldTypes",
            "DynamicMobileScanTypeAuditTemplateFieldTypes"
    };

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final ArrayNode getLookupTypes() {

        //String[] lookups = Stream.of(FoDLookupType.values()).map(FoDLookupType::name).toArray();
        List<String> lookups = Stream.of(FoDLookupTypeOptions.FoDLookupType.values())
                .map(FoDLookupTypeOptions.FoDLookupType::name)
                .collect(Collectors.toList());
        System.out.println(lookups);
        ArrayNode n = objectMapper.valueToTree(lookups);
        System.out.println(n.toPrettyString());
        return n;
//        return JsonHelper.toArrayNode(lookups.toArray(String[]::new));
    }

    public static final FoDLookupDescriptor getDescriptor(UnirestInstance unirestInstance, FoDLookupTypeOptions.FoDLookupType type,
                                                          String text, boolean failIfNotFound) throws JsonProcessingException {
        FoDLookupDescriptor currentLookup = null;
        GetRequest request = unirestInstance.get(FoDUrls.LOOKUP_ITEMS).queryString("type",
                type.name());
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDLookupDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDLookupDescriptor>>() {
                });
        Iterator<FoDLookupDescriptor> lookupIterator = lookupList.iterator();
        while (lookupIterator.hasNext()) {
            currentLookup = lookupIterator.next();
            if (currentLookup.getText().equals(text)) {
                break;
            }
        }
        if (failIfNotFound) {
            throw new ValidationException("No value found for '" + text + "' in " + type.name());
        }
        return currentLookup;
    }

    public static final FoDLookupDescriptor getDescriptor(UnirestInstance unirestInstance, FoDLookupTypeOptions.FoDLookupType type,
                                                          String group, String text, boolean failIfNotFound) throws JsonProcessingException {
        FoDLookupDescriptor currentLookup = null;
        GetRequest request = unirestInstance.get(FoDUrls.LOOKUP_ITEMS).queryString("type",
                type.name());
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDLookupDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDLookupDescriptor>>() {
                });
        Iterator<FoDLookupDescriptor> lookupIterator = lookupList.iterator();
        while (lookupIterator.hasNext()) {
            currentLookup = lookupIterator.next();
            if (currentLookup.getGroup().equals(group) && currentLookup.getText().equals(text)) {
                break;
            }
        }
        if (failIfNotFound) {
            throw new ValidationException("No value found for '" + text + "' with group '" + group + "' in " + type.name());
        }
        return currentLookup;
    }
}
