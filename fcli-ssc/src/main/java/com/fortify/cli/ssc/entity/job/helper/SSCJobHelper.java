/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.job.helper;

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
