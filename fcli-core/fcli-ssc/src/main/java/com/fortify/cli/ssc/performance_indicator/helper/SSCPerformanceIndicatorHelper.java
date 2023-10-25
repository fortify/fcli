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
package com.fortify.cli.ssc.performance_indicator.helper;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.UnirestInstance;

public final class SSCPerformanceIndicatorHelper {
    private final Map<String, SSCPerformanceIndicatorDescriptor> descriptorsById = new HashMap<>();
    private final Map<String, SSCPerformanceIndicatorDescriptor> descriptorsByGuid = new HashMap<>();
    private final Map<String, SSCPerformanceIndicatorDescriptor> descriptorsByName = new HashMap<>();
    
    /**
     * This constructor calls the SSC projectVersion filterSets endpoint to retrieve filter set data,
     * then calls the {@link #processPerformanceIndicator(JsonNode)} method for each filter set to collect the
     * relevant data.
     * @param unirest
     */
    public SSCPerformanceIndicatorHelper(UnirestInstance unirest, String applicationVersionId) {
        JsonNode body = unirest.get(SSCUrls.PROJECT_VERSION_PERFORMANCE_INDICATOR_HISTORIES(applicationVersionId)).queryString("limit","-1").asObject(JsonNode.class).getBody();
        body.get("data").forEach(this::processPerformanceIndicator);
    }

    private void processPerformanceIndicator(JsonNode issueTemplate) {
        SSCPerformanceIndicatorDescriptor descriptor = JsonHelper.treeToValue(issueTemplate, SSCPerformanceIndicatorDescriptor.class);
        descriptorsById.put(descriptor.getId(), descriptor);
        descriptorsByGuid.put(descriptor.getGuid(), descriptor);
        descriptorsByName.put(descriptor.getName(), descriptor);
    }
    
    public SSCPerformanceIndicatorDescriptor getDescriptorByNameOrIdOrGuid(String performanceIndicatorNameOrIdOrGuid, boolean failIfNotFound) {
        SSCPerformanceIndicatorDescriptor descriptor = StringUtils.isBlank(performanceIndicatorNameOrIdOrGuid) ? null : descriptorsById.get(performanceIndicatorNameOrIdOrGuid);
        if ( descriptor==null ) {
            descriptor = descriptorsByGuid.get(performanceIndicatorNameOrIdOrGuid);
        }
        if ( descriptor==null ) {
            descriptor = descriptorsByName.get(performanceIndicatorNameOrIdOrGuid);
        }
        if ( failIfNotFound && descriptor==null ) {
            throw new IllegalArgumentException("No Performance Indicator found with name, id or guid "+performanceIndicatorNameOrIdOrGuid);
        }
        return descriptor;
    }

    public static final JsonNode transformRecord(JsonNode record) {
        return ((ObjectNode)record).put("valueString", record.get("value").asText()+getValueSuffix(record));
    }

    private static String getValueSuffix(JsonNode record) {
        switch (record.get("range").asText()) {
        case "percent": return "%";
        default: return "";
        }
    }
}
