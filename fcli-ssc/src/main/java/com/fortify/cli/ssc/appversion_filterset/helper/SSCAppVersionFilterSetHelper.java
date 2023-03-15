package com.fortify.cli.ssc.appversion_filterset.helper;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;

public final class SSCAppVersionFilterSetHelper {
    private final Map<String, SSCAppVersionFilterSetDescriptor> descriptorsByGuid = new HashMap<>();
    private final Map<String, SSCAppVersionFilterSetDescriptor> descriptorsByTitle = new HashMap<>();
    @Getter private SSCAppVersionFilterSetDescriptor defaultFilterSetDescriptor;
    
    /**
     * This constructor calls the SSC projectVersion filterSets endpoint to retrieve filter set data,
     * then calls the {@link #processFilterSet(JsonNode)} method for each filter set to collect the
     * relevant data.
     * @param unirest
     */
    public SSCAppVersionFilterSetHelper(UnirestInstance unirest, String applicationVersionId) {
        JsonNode body = unirest.get(SSCUrls.PROJECT_VERSION_FILTER_SETS(applicationVersionId)).queryString("limit","-1").asObject(JsonNode.class).getBody();
        body.get("data").forEach(this::processFilterSet);
    }

    private void processFilterSet(JsonNode issueTemplate) {
        SSCAppVersionFilterSetDescriptor descriptor = JsonHelper.treeToValue(issueTemplate, SSCAppVersionFilterSetDescriptor.class);
        descriptorsByGuid.put(descriptor.getGuid(), descriptor);
        descriptorsByTitle.put(descriptor.getTitle(), descriptor);
        if ( descriptor.isDefaultFilterSet() ) {
            this.defaultFilterSetDescriptor = descriptor;
        }
    }
    
    public SSCAppVersionFilterSetDescriptor getDescriptorByTitleOrId(String filterSetTitleOrId, boolean failIfNotFound) {
        if ( filterSetTitleOrId==null ) { return defaultFilterSetDescriptor; }
        SSCAppVersionFilterSetDescriptor descriptor = descriptorsByGuid.get(filterSetTitleOrId);
        descriptor = descriptor!=null ? descriptor : descriptorsByTitle.get(filterSetTitleOrId);
        if ( failIfNotFound && descriptor==null ) {
            throw new IllegalArgumentException("No filter set found with title or id "+filterSetTitleOrId);
        }
        return descriptor;
    }
}
