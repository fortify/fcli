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
package com.fortify.cli.ssc.role.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.runner.UnexpectedHttpResponseException;
import com.fortify.cli.ssc.rest.SSCUrls;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

import javax.validation.ValidationException;

public class SSCRoleHelper {
    public static final SSCRoleDescriptor getRole(UnirestInstance unirestInstance, String roleNameOrId, String... fields) {
        GetRequest request = unirestInstance.get(SSCUrls.ROLE(roleNameOrId));

        try{
            if ( fields!=null && fields.length>0 ) {
                request.queryString("fields", String.join(",", fields));
            }

            JsonNode role = request.asObject(ObjectNode.class).getBody().get("data");
            return JsonHelper.treeToValue(role, SSCRoleDescriptor.class);

        }catch (UnexpectedHttpResponseException | NullPointerException e){
            try{
                request = unirestInstance.get(SSCUrls.ROLES)
                        .queryString("q", String.format("name:\"%s\"",roleNameOrId))
                        .queryString("limit", "2");

                if ( fields!=null && fields.length>0 ) {
                    request.queryString("fields", String.join(",", fields));
                }

                JsonNode roles = request.asObject(ObjectNode.class).getBody().get("data");
                if ( roles.size()==0 ) {
                    throw new ValidationException("No role found for the role name or id: " + roleNameOrId);
                } else if ( roles.size()>1 ) {
                    throw new ValidationException("Multiple roles found for the role name or id: " + roleNameOrId);
                }
                return JsonHelper.treeToValue(roles.get(0), SSCRoleDescriptor.class);

            }catch (UnexpectedHttpResponseException | NullPointerException ee){
                throw new ValidationException("Unable to find the specified role: " + roleNameOrId);
            }
        }
    }
}
