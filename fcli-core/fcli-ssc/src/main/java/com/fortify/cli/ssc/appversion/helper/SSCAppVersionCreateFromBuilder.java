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
package com.fortify.cli.ssc.appversion.helper;

import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.access_control.helper.SSCAppVersionUserUpdateBuilder;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

import java.util.HashMap;

public final class SSCAppVersionCreateFromBuilder {
    private final UnirestInstance unirest;

    private String previousProjectVersionId;

    public SSCAppVersionCreateFromBuilder(UnirestInstance unirest) {
        this.unirest = unirest;
    }


    public final HttpRequest<?> buildRequest(String projectVersionId) {
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("previousProjectVersionId", this.previousProjectVersionId);
        body.put("projectVersionId", projectVersionId);
        body.put("copyAnalysisProcessingRules", "true");
        body.put("copyBugTrackerConfiguration", "true");
        body.put("copyCustomTags", "true");
        return unirest
                .post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_FROM_PARTIAL)
                .body(body);
    }

    public final SSCAppVersionCreateFromBuilder set(String previousProjectVersionId) {
        this.previousProjectVersionId = previousProjectVersionId;
        return this;
    }
}
