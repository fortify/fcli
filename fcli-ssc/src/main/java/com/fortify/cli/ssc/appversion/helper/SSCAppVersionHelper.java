/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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
