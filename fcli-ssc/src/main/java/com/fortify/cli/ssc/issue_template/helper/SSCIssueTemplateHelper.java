package com.fortify.cli.ssc.issue_template.helper;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.util.JsonHelper;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;

public final class SSCIssueTemplateHelper {
    private final Map<String, SSCIssueTemplateDescriptor> descriptorsByLowerId = new HashMap<>();
    private final Map<String, SSCIssueTemplateDescriptor> descriptorsByLowerName = new HashMap<>();
    @Getter private SSCIssueTemplateDescriptor defaultIssueTemplateDescriptor;
    
    /**
     * This constructor calls the SSC projectTemplates endpoint to retrieve issue template data,
     * then calls the {@link #processIssueTemplate(JsonNode)} method for each issue template.
     * @param unirest
     */
    public SSCIssueTemplateHelper(UnirestInstance unirest) {
        JsonNode issueTemplatesBody = unirest.get(SSCUrls.ISSUE_TEMPLATES).queryString("limit","-1").asObject(JsonNode.class).getBody();
        issueTemplatesBody.get("data").forEach(this::processIssueTemplate);
    }

    private void processIssueTemplate(JsonNode issueTemplate) {
        SSCIssueTemplateDescriptor descriptor = JsonHelper.treeToValue(issueTemplate, SSCIssueTemplateDescriptor.class);
        descriptorsByLowerId.put(descriptor.getId().toLowerCase(), descriptor);
        // TODO Potentially we could have multiple templates with equal lowercase names,
        //      but we neglect this risk for now as it is very unlikely
        descriptorsByLowerName.put(descriptor.getName().toLowerCase(), descriptor);
        if ( descriptor.isDefaultTemplate() ) {
            this.defaultIssueTemplateDescriptor = descriptor;
        }
    }
    
    public SSCIssueTemplateDescriptor getDescriptorByNameOrId(String issueTemplateNameOrId, boolean failIfNotFound) {
        SSCIssueTemplateDescriptor descriptor = descriptorsByLowerId.get(issueTemplateNameOrId.toLowerCase());
        descriptor = descriptor!=null ? descriptor : descriptorsByLowerName.get(issueTemplateNameOrId.toLowerCase());
        if ( failIfNotFound && descriptor==null ) {
            throw new IllegalArgumentException("No issue template found with name or id "+issueTemplateNameOrId);
        }
        return descriptor;
    }
    
    /**
     * If only the default issue template is needed, then this method will be more performant than
     * new SSCIssueTemplateHelper(unirest).getDefaultIssueTemplateDescriptor().
     * @param unirest
     * @return
     */
    public static final SSCIssueTemplateDescriptor getDefaultIssueTemplateDescriptor(UnirestInstance unirest) {
        JsonNode issueTemplatesBody = unirest.get(SSCUrls.ISSUE_TEMPLATES)
                .queryString("limit","-1")
                .queryString("q", "defaultTemplate:true")
                .asObject(JsonNode.class).getBody();
        return JsonHelper.treeToValue(issueTemplatesBody.get("data").get(0), SSCIssueTemplateDescriptor.class);
    }
    
}
