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
package com.fortify.cli.ssc.issue.helper;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.SSCUrls;

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
    
    public SSCIssueTemplateDescriptor getIssueTemplateDescriptorOrDefault(String issueTemplateNameOrId) {
        return StringUtils.isBlank(issueTemplateNameOrId)
                ? getDefaultIssueTemplateDescriptor()
                : getDescriptorByNameOrId(issueTemplateNameOrId, true);
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
