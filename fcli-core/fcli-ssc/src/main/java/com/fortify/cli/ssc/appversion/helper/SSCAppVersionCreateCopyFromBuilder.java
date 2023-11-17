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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public final class SSCAppVersionCreateCopyFromBuilder {
    private final UnirestInstance unirest;

    private ObjectNode copyFromPartialOptions = JsonHelper.getObjectMapper().createObjectNode();
    private ObjectNode copyStateOptions = JsonHelper.getObjectMapper().createObjectNode();
    private SSCAppVersionDescriptor previousProjectVersion;

    private boolean copyRequested = false;

    private boolean copyState = false;

    public SSCAppVersionCreateCopyFromBuilder(UnirestInstance unirest) {
        this.unirest = unirest;
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

        this.copyStateOptions.put("projectVersionId", Integer.parseInt(projectVersionId));

        ObjectNode body = JsonHelper.getObjectMapper().createObjectNode();
        body    .put("type", "copy_current_state")
                .set("values", copyStateOptions);

        return unirest
                .post(SSCUrls.PROJECT_VERSIONS_ACTION(projectVersionId))
                .body(body);
    }

    public final SSCAppVersionCreateCopyFromBuilder setCopyFrom(SSCAppVersionDescriptor previousProjectVersionDescriptor) {
        this.previousProjectVersion = previousProjectVersionDescriptor;
        this.copyFromPartialOptions.put("previousProjectVersionId", previousProjectVersionDescriptor.getVersionId());
        this.copyStateOptions.put("previousProjectVersionId", previousProjectVersionDescriptor.getIntVersionId());
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
