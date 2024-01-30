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
package com.fortify.cli.ssc.report.helper;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.UnirestInstance;

public final class SSCReportTemplateHelper {
    private final Map<Integer, SSCReportTemplateDescriptor> descriptorsById = new HashMap<>();
    private final Map<String, SSCReportTemplateDescriptor> descriptorsByName = new HashMap<>();
    
    /**
     * This constructor calls the SSC reportTemplates endpoint to retrieve report template data,
     * then calls the {@link #processReportTemplate(JsonNode)} method for each report template to 
     * collect the relevant data.
     * @param unirest
     */
    public SSCReportTemplateHelper(UnirestInstance unirest) {
        // TODO Do we want to include all fields or just a subset like name, id and fileName? 
        //      We want to include all data in the 'get' command, so either we include all data
        //      here, or we can do an extra call to SSC to get the full details after resolving
        //      the report template id.
        JsonNode body = unirest.get(SSCUrls.REPORT_DEFINITIONS)
                .queryString("limit","-1")
                .asObject(JsonNode.class).getBody();
        body.get("data").forEach(this::processReportTemplate);
    }

    private void processReportTemplate(JsonNode reportTemplate) {
        SSCReportTemplateDescriptor descriptor = JsonHelper.treeToValue(reportTemplate, SSCReportTemplateDescriptor.class);
        descriptorsById.put(descriptor.getId(), descriptor);
        descriptorsByName.put(descriptor.getName(), descriptor);
    }
    
    public SSCReportTemplateDescriptor getDescriptorByNameOrId(String reportTemplateNameOrId, boolean failIfNotFound) {
        SSCReportTemplateDescriptor descriptor = null;
        try { descriptor = descriptorsById.get(Integer.parseInt(reportTemplateNameOrId)); }
        catch ( NumberFormatException ignore ) {}
        descriptor = descriptor!=null ? descriptor : descriptorsByName.get(reportTemplateNameOrId);
        if ( failIfNotFound && descriptor==null ) {
            throw new IllegalArgumentException("No report template found with name or id "+reportTemplateNameOrId);
        }
        return descriptor;
    }
}
