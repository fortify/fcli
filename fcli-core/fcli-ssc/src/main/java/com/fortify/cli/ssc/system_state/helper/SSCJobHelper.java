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
package com.fortify.cli.ssc.system_state.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.wait.WaitHelper;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.common.rest.wait.WaitType;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;


public final class SSCJobHelper   {
    public static final SSCJobDescriptor getJobDescriptor(UnirestInstance unirest, String jobName, String... fields) {
        if ( StringUtils.isBlank(jobName) ) { return null; }
        GetRequest request = unirest.get(SSCUrls.JOB(jobName));
        if ( fields!=null && fields.length>0 ) {
            request = request.queryString("fields", String.join(",",fields));
        }
        JsonNode jobBody = request.asObject(JsonNode.class).getBody();
        return JsonHelper.treeToValue(jobBody.get("data"), SSCJobDescriptor.class);
    }

    private static JsonNode getJobJsonNode(UnirestInstance unirest, String jobName) {
        return unirest.get(SSCUrls.JOB(jobName))
                .asObject(JsonNode.class).getBody().get("data");
    }

    private static final SSCJobDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, SSCJobDescriptor.class);
    }

    public static final SSCJobDescriptor getJobDescriptor(UnirestInstance unirest, String jobName) {
        return StringUtils.isBlank(jobName) ? null : getDescriptor(getJobJsonNode(unirest, jobName));
    }

    public final static SSCJobDescriptor waitForJob(UnirestInstance unirest, SSCJobDescriptor descriptor, String timeout){
        WaitHelperBuilder builder = WaitHelper.builder()
                .waitType(new WaitType(WaitType.LoopType.Until, WaitType.AnyOrAll.all_match))
                .timeoutPeriod(StringUtils.isBlank(timeout)?"60s":timeout)
                .intervalPeriod("5s")
                .onFailureState(null)
                .onTimeout(null)
                .onUnknownState(null)
                .onUnknownStateRequested(null);

        builder .recordsSupplier(u->Collections.singletonList(getJobJsonNode(u, descriptor.getJobName())))
                .currentStateProperty("state")
                .knownStates(SSCJobStatus.getKnownStateNames())
                .failureStates(SSCJobStatus.getFailureStateNames())
                .matchStates(new HashSet<>(Arrays.asList(SSCJobStatus.getDefaultCompleteStateNames())));

        builder.build().wait(unirest);

        return descriptor;
    }

}
