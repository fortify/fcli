package com.fortify.cli.ssc.issue_template.helper;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;

public final class SSCIssueTemplateHelper {
    private final Map<String, SSCIssueTemplateDescriptor> descriptorsById = new HashMap<>();
    private final Map<String, SSCIssueTemplateDescriptor> descriptorsByName = new HashMap<>();
    @Getter private SSCIssueTemplateDescriptor defaultIssueTemplateDescriptor;
    
    /**
     * This constructor calls the SSC projectTemplates endpoint to retrieve issue template data,
     * then calls the {@link #processIssueTemplate(JsonNode)} method for each issue template to 
     * collect the relevant data.
     * @param unirest
     */
    public SSCIssueTemplateHelper(UnirestInstance unirest) {
        JsonNode issueTemplatesBody = unirest.get(SSCUrls.ISSUE_TEMPLATES).queryString("limit","-1").asObject(JsonNode.class).getBody();
        issueTemplatesBody.get("data").forEach(this::processIssueTemplate);
    }

    private void processIssueTemplate(JsonNode issueTemplate) {
        SSCIssueTemplateDescriptor descriptor = JsonHelper.treeToValue(issueTemplate, SSCIssueTemplateDescriptor.class);
        descriptorsById.put(descriptor.getId(), descriptor);
        descriptorsByName.put(descriptor.getName(), descriptor);
        if ( descriptor.isDefaultTemplate() ) {
            this.defaultIssueTemplateDescriptor = descriptor;
        }
    }
    
    public SSCIssueTemplateDescriptor getDescriptorByNameOrId(String issueTemplateNameOrId, boolean failIfNotFound) {
        SSCIssueTemplateDescriptor descriptor = descriptorsById.get(issueTemplateNameOrId);
        descriptor = descriptor!=null ? descriptor : descriptorsByName.get(issueTemplateNameOrId);
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
