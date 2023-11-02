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
import com.fortify.cli.ssc.appversion.cli.mixin.SSCCopyOptions;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

import java.util.HashMap;

public final class SSCAppVersionCreateCopyFromBuilder {
    private final UnirestInstance unirest;

    private HashMap<String, String> copyFromPartialOptions = new HashMap<String, String>();
    private HashMap<String, String> copyStateOptions = new HashMap<String, String>();

    private boolean copyState;

    public SSCAppVersionCreateCopyFromBuilder(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    public final HttpRequest<?> buildCopyFromPartialRequest(String projectVersionId) {
        this.copyFromPartialOptions.put("projectVersionId", projectVersionId);
        return unirest
                .post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_FROM_PARTIAL)
                .body(copyFromPartialOptions);
    }


    public final HttpRequest<?> buildCopyStateRequest(String projectVersionId) {
        this.copyStateOptions.put("projectVersionId", projectVersionId);
        return unirest
                .post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_CURRENT_STATE)
                .body(copyStateOptions);
    }

    public final SSCAppVersionCreateCopyFromBuilder setCopyFrom(String previousProjectVersionId) {
        this.copyFromPartialOptions.put("previousProjectVersionId", previousProjectVersionId);
        this.copyStateOptions.put("previousProjectVersionId", previousProjectVersionId);
        return this;
    }

    public final SSCAppVersionCreateCopyFromBuilder setCopyOptions(SSCCopyOptions.SSCCopyOption[] copyOptions) {
        if(copyOptions == null) {
            copyOptions = SSCCopyOptions.SSCCopyOption.values();
        }
        for (SSCCopyOptions.SSCCopyOption option : copyOptions) {
            if(option.getSscValue() == "copyState") {
                this.copyState = true;
            } else {
                this.copyFromPartialOptions.put(option.getSscValue(), "true");
            }
        }

        return this;
    }

    public boolean copyStateEnabled() {
        return this.copyState;
    }
}
