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
package com.fortify.cli.ssc.app.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.core.GetRequest;
import kong.unirest.core.UnirestInstance;

public class SSCAppHelper {
    public static final SSCAppDescriptor getApp(UnirestInstance unirestInstance, String appNameOrId, boolean failIfNotFound, String... fields) {
        GetRequest request = unirestInstance.get("/api/v1/projects?limit=2");
        if ( fields!=null && fields.length>0 ) {
            request.queryString("fields", String.join(",", fields));
        }
        
        try {
            int appId = Integer.parseInt(appNameOrId);
            request = request.queryString("q", String.format("id:%d", appId));
        } catch (NumberFormatException nfe) {
            request = request.queryString("q", String.format("name:\"%s\"", appNameOrId));
        }
        JsonNode apps = request.asObject(ObjectNode.class).getBody().get("data");
        if ( failIfNotFound && apps.size()==0 ) {
            throw new IllegalArgumentException("No application found for application name or id: "+appNameOrId);
        } else if ( apps.size()>1 ) {
            throw new IllegalArgumentException("Multiple applications found for application name or id: "+appNameOrId);
        }
        return apps.size()==0 ? null : JsonHelper.treeToValue(apps.get(0), SSCAppDescriptor.class);
    }
}
