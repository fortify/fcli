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
package com.fortify.cli.ssc.variable.helper;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.UnirestInstance;

public final class SSCVariableHelper {
    private final Map<String, SSCVariableDescriptor> descriptorsById = new HashMap<>();
    private final Map<String, SSCVariableDescriptor> descriptorsByGuid = new HashMap<>();
    private final Map<String, SSCVariableDescriptor> descriptorsByName = new HashMap<>();
    
    public SSCVariableHelper(UnirestInstance unirest, String applicationVersionId) {
        JsonNode body = unirest.get(SSCUrls.PROJECT_VERSION_VARIABLE_HISTORIES(applicationVersionId)).queryString("limit","-1").asObject(JsonNode.class).getBody();
        body.get("data").forEach(this::processPerformanceIndicator);
    }

    private void processPerformanceIndicator(JsonNode issueTemplate) {
        SSCVariableDescriptor descriptor = JsonHelper.treeToValue(issueTemplate, SSCVariableDescriptor.class);
        descriptorsById.put(descriptor.getId(), descriptor);
        descriptorsByGuid.put(descriptor.getGuid(), descriptor);
        descriptorsByName.put(descriptor.getName(), descriptor);
    }
    
    public SSCVariableDescriptor getDescriptorByNameOrIdOrGuid(String variableNameOrIdOrGuid, boolean failIfNotFound) {
        SSCVariableDescriptor descriptor = StringUtils.isBlank(variableNameOrIdOrGuid) ? null : descriptorsById.get(variableNameOrIdOrGuid);
        if ( descriptor==null ) {
            descriptor = descriptorsByGuid.get(variableNameOrIdOrGuid);
        }
        if ( descriptor==null ) {
            descriptor = descriptorsByName.get(variableNameOrIdOrGuid);
        }
        if ( failIfNotFound && descriptor==null ) {
            throw new IllegalArgumentException("No variable found with name, id or guid "+variableNameOrIdOrGuid);
        }
        return descriptor;
    }
}
