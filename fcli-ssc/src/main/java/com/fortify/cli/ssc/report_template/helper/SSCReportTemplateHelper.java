package com.fortify.cli.ssc.report_template.helper;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;

public final class SSCReportTemplateHelper {
    private final Map<String, SSCReportTemplateDescriptor> descriptorsByLowerId = new HashMap<>();
    private final Map<String, SSCReportTemplateDescriptor> descriptorsByLowerName = new HashMap<>();
    
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
        descriptorsByLowerId.put(descriptor.getId().toLowerCase(), descriptor);
        // TODO Potentially we could have multiple templates with equal lowercase names,
        //      but we neglect this risk for now as it is very unlikely
        descriptorsByLowerName.put(descriptor.getName().toLowerCase(), descriptor);
    }
    
    public SSCReportTemplateDescriptor getDescriptorByNameOrId(String reportTemplateNameOrId, boolean failIfNotFound) {
        SSCReportTemplateDescriptor descriptor = descriptorsByLowerId.get(reportTemplateNameOrId.toLowerCase());
        descriptor = descriptor!=null ? descriptor : descriptorsByLowerName.get(reportTemplateNameOrId.toLowerCase());
        if ( failIfNotFound && descriptor==null ) {
            throw new IllegalArgumentException("No report template found with name or id "+reportTemplateNameOrId);
        }
        return descriptor;
    }
}
