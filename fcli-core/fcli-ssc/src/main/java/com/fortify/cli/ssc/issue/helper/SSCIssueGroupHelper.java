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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.UnirestInstance;

public final class SSCIssueGroupHelper {
    private final Map<String, SSCIssueGroupDescriptor> descriptorsByGuid = new HashMap<>();
    private final Map<String, SSCIssueGroupDescriptor> descriptorsByDisplayName = new HashMap<>();
    
    /**
     * This constructor calls the SSC projectVersion issueSelectorSet endpoint to retrieve filter and grouping data.
     * As we are only interested in the grouping data, we then call the {@link #processGroupSet(JsonNode)} method for
     * each grouping to collect the relevant data.
     * @param unirest
     */
    public SSCIssueGroupHelper(UnirestInstance unirest, String applicationVersionId) {
        JsonNode body = unirest.get(SSCUrls.PROJECT_VERSION_ISSUE_SELECTOR_SET(applicationVersionId)).asObject(JsonNode.class).getBody();
        body.get("data").get("groupBySet").forEach(this::processGroupSet);
    }

    private void processGroupSet(JsonNode issueTemplate) {
        SSCIssueGroupDescriptor descriptor = JsonHelper.treeToValue(issueTemplate, SSCIssueGroupDescriptor.class);
        descriptorsByGuid.put(descriptor.getGuid(), descriptor);
        descriptorsByDisplayName.put(descriptor.getDisplayName(), descriptor);
    }
    
    public SSCIssueGroupDescriptor getDescriptorByDisplayNameOrId(String groupBySetDisplayNameOrId, boolean failIfNotFound) {
        SSCIssueGroupDescriptor descriptor = descriptorsByGuid.get(groupBySetDisplayNameOrId);
        descriptor = descriptor!=null ? descriptor : descriptorsByDisplayName.get(groupBySetDisplayNameOrId);
        if ( failIfNotFound && descriptor==null ) {
            throw new IllegalArgumentException("No grouping found with display name or id "+groupBySetDisplayNameOrId);
        }
        return descriptor;
    }
}
