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
package com.fortify.cli.ssc.appversion_performanceindicator.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public final class SSCAppVersionPerformanceIndicatorHelper {
    private final Map<String, SSCAppVersionPerformanceIndicatorDescriptor> descriptorsById = new HashMap<>();
    @Getter private SSCAppVersionPerformanceIndicatorDescriptor defaultPerformanceIndicatorDescriptor;
    
    /**
     * This constructor calls the SSC projectVersion filterSets endpoint to retrieve filter set data,
     * then calls the {@link #processPerformanceIndicator(JsonNode)} method for each filter set to collect the
     * relevant data.
     * @param unirest
     */
    public SSCAppVersionPerformanceIndicatorHelper(UnirestInstance unirest, String applicationVersionId) {
        JsonNode body = unirest.get(SSCUrls.PROJECT_VERSION_PERFORMANCE_INDICATOR_HISTORIES(applicationVersionId)).queryString("limit","-1").asObject(JsonNode.class).getBody();
        body.get("data").forEach(this::processPerformanceIndicator);
    }

    private void processPerformanceIndicator(JsonNode issueTemplate) {
        SSCAppVersionPerformanceIndicatorDescriptor descriptor = JsonHelper.treeToValue(issueTemplate, SSCAppVersionPerformanceIndicatorDescriptor.class);
        descriptorsById.put(descriptor.getId(), descriptor);
        if ( descriptor.isDefaultPerformanceIndicator() ) {
            this.defaultPerformanceIndicatorDescriptor = descriptor;
        }
    }
    
    public SSCAppVersionPerformanceIndicatorDescriptor getDescriptorById(String performanceIndicatorId, boolean failIfNotFound) {
        if ( performanceIndicatorId==null ) { return defaultPerformanceIndicatorDescriptor; }
        SSCAppVersionPerformanceIndicatorDescriptor descriptor = descriptorsById.get(performanceIndicatorId);
        if ( failIfNotFound && descriptor==null ) {
            throw new IllegalArgumentException("No Performance Indicator found with id "+performanceIndicatorId);
        }
        return descriptor;
    }
}
