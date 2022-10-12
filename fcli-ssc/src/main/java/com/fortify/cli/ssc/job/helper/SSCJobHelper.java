package com.fortify.cli.ssc.job.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

public final class SSCJobHelper {
    public static final SSCJobDescriptor getJobDescriptor(UnirestInstance unirest, String jobName, String... fields) {
        GetRequest request = unirest.get(SSCUrls.JOB(jobName));
        if ( fields!=null && fields.length>0 ) {
            request = request.queryString("fields", String.join(",",fields));
        }
        JsonNode jobBody = request.asObject(JsonNode.class).getBody();
        return JsonHelper.treeToValue(jobBody.get("data"), SSCJobDescriptor.class);
    }
}
