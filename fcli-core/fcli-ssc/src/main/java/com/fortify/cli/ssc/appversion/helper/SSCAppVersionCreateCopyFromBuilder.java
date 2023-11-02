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

import java.util.HashMap;

import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.system_state.helper.SSCJobDescriptor;
import com.fortify.cli.ssc.system_state.helper.SSCJobHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public final class SSCAppVersionCreateCopyFromBuilder {
    private final UnirestInstance unirest;

    private HashMap<String, String> copyFromPartialOptions = new HashMap<String, String>();
    private HashMap<String, String> copyStateOptions = new HashMap<String, String>();
    private SSCAppVersionDescriptor previousProjectVersion;

    private boolean copyRequested = false;

    private boolean copyState;

    public SSCAppVersionCreateCopyFromBuilder(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    public final HttpRequest<?> buildCopyFromRefreshRequest(String projectVersionId) {
        if(!copyState || !copyRequested){
            return null;
        }

        this.copyFromPartialOptions.put("projectVersionId", projectVersionId);
        return unirest
                .post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_FROM_PARTIAL)
                .body(copyFromPartialOptions);
    }

    public final HttpRequest<?> buildCopyFromPartialRequest(String projectVersionId) {
        if(!copyRequested){
            return null;
        }

        this.copyFromPartialOptions.put("projectVersionId", projectVersionId);
        return unirest
                .post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_FROM_PARTIAL)
                .body(copyFromPartialOptions);
    }

    public final HttpRequest<?> buildCopyStateRequest(String projectVersionId) {
        if(!copyState || !copyRequested){
            return null;
        }
        // refreshMetrics if the source PV is required to fully copy the tags, audit or comments
        if(this.previousProjectVersion.isRefreshRequired()){
            SSCJobDescriptor refreshJobDesc = SSCAppVersionHelper.refreshMetrics(unirest, this.previousProjectVersion);
            SSCJobHelper.waitForJob(unirest,refreshJobDesc);
        }
        this.copyStateOptions.put("projectVersionId", projectVersionId);
        return unirest
                .post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_CURRENT_STATE)
                .body(copyStateOptions);
    }

    public final SSCAppVersionCreateCopyFromBuilder setCopyFrom(SSCAppVersionDescriptor previousProjectVersionDescriptor) {
        this.previousProjectVersion = previousProjectVersionDescriptor;
        this.copyFromPartialOptions.put("previousProjectVersionId", previousProjectVersionDescriptor.getVersionId());
        this.copyStateOptions.put("previousProjectVersionId", previousProjectVersionDescriptor.getVersionId());
        return this;
    }

    public final SSCAppVersionCreateCopyFromBuilder setCopyOptions(SSCAppVersionCopyType[] copyOptions) {
        if(copyOptions == null) {
            copyOptions = SSCAppVersionCopyType.values();
        }
        for (SSCAppVersionCopyType option : copyOptions) {
            if(option.getSscValue() == "copyState") {
                this.copyState = true;
            } else {
                this.copyFromPartialOptions.put(option.getSscValue(), "true");
            }
        }

        return this;
    }

    public final SSCAppVersionCreateCopyFromBuilder setCopyRequested(boolean status){
        this.copyRequested = status;

        return this;
    }

    public boolean copyStateEnabled() {
        return this.copyState;
    }
}
