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

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

public class SSCAppVersionHelper {
    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[] {"project:application"}).transform(record);
    }
    
    public static final SSCAppVersionDescriptor getRequiredAppVersion(UnirestInstance unirest, String appVersionNameOrId, String delimiter, String... fields) {
        SSCAppVersionDescriptor descriptor = getOptionalAppVersion(unirest, appVersionNameOrId, delimiter, fields);
        if ( descriptor==null ) {
            throw new ValidationException("No application version found for application version name or id: "+appVersionNameOrId);
        }
        return descriptor;
    }
    
    public static final SSCAppVersionDescriptor getOptionalAppVersion(UnirestInstance unirest, String appVersionNameOrId, String delimiter, String... fields) {
        try {
            int versionId = Integer.parseInt(appVersionNameOrId);
            return getOptionalAppVersionFromId(unirest, versionId, fields);
        } catch (NumberFormatException nfe) {
            return getOptionalAppVersionFromAppAndVersionName(unirest, SSCAppAndVersionNameDescriptor.fromCombinedAppAndVersionName(appVersionNameOrId, delimiter), fields);
        }
    }
    
    public static final SSCAppVersionDescriptor getOptionalAppVersionFromId(UnirestInstance unirest, int versionId, String... fields) {
        GetRequest request = getBaseRequest(unirest, fields).queryString("q", String.format("id:%d", versionId));
        return getOptionalDescriptor(request);
    }
    
    public static final SSCAppVersionDescriptor getOptionalAppVersionFromAppAndVersionName(UnirestInstance unirest, SSCAppAndVersionNameDescriptor appAndVersionNameDescriptor, String... fields) {
        GetRequest request = getBaseRequest(unirest, fields);
        request = request.queryString("q", String.format("project.name:\"%s\",name:\"%s\"", appAndVersionNameDescriptor.getAppName(), appAndVersionNameDescriptor.getVersionName()));
        return getOptionalDescriptor(request);
    }

    private static GetRequest getBaseRequest(UnirestInstance unirest, String... fields) {
        GetRequest request = unirest.get("/api/v1/projectVersions?limit=2");
        if ( fields!=null && fields.length>0 ) {
            request.queryString("fields", String.join(",", fields));
        }
        return request;
    }

    private static final SSCAppVersionDescriptor getOptionalDescriptor(GetRequest request) {
        JsonNode versions = request.asObject(ObjectNode.class).getBody().get("data");
        if ( versions.size()>1 ) {
            throw new ValidationException("Multiple application versions found");
        }
        return versions.size()==0 ? null : JsonHelper.treeToValue(versions.get(0), SSCAppVersionDescriptor.class);
    }
}
