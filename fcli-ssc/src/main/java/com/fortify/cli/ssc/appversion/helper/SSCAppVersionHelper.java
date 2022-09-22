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

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

public class SSCAppVersionHelper {
    public static final SSCAppVersionDescriptor getAppVersion(UnirestInstance unirestInstance, String appVersionNameOrId, String delimiter, String... fields) {
        GetRequest request = unirestInstance.get("/api/v1/projectVersions?limit=2");
        if ( fields!=null && fields.length>0 ) {
            request.queryString("fields", String.join(",", fields));
        }
            
        try {
            int versionId = Integer.parseInt(appVersionNameOrId);
            request = request.queryString("q", String.format("id:%d", versionId));
        } catch (NumberFormatException nfe) {
            String[] appAndVersionName = appVersionNameOrId.split(delimiter);
            if ( appAndVersionName.length != 2 ) { 
                throw new ValidationException("Application version must be specified as either numeric version id, or in the format <application name>"+delimiter+"<version name>"); 
            }
            request = request.queryString("q", String.format("project.name:\"%s\",name:\"%s\"", appAndVersionName[0], appAndVersionName[1]));
        }
        JsonNode versions = request.asObject(ObjectNode.class).getBody().get("data");
        if ( versions.size()==0 ) {
            throw new ValidationException("No application version found for application version name or id: "+appVersionNameOrId);
        } else if ( versions.size()>1 ) {
            throw new ValidationException("Multiple application versions found for application version name or id: "+appVersionNameOrId);
        }
        return JsonHelper.treeToValue(versions.get(0), SSCAppVersionDescriptor.class);
    }
}
