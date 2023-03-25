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
package com.fortify.cli.ssc.entity.app.helper;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

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
            throw new ValidationException("No application found for application name or id: "+appNameOrId);
        } else if ( apps.size()>1 ) {
            throw new ValidationException("Multiple applications found for application name or id: "+appNameOrId);
        }
        return apps.size()==0 ? null : JsonHelper.treeToValue(apps.get(0), SSCAppDescriptor.class);
    }
}
