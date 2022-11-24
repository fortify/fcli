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

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.runner.UnexpectedHttpResponseException;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

public class SSCRoleHelper {
    public static final SSCRoleDescriptor getRoleDescriptor(UnirestInstance unirestInstance, String roleNameOrId, String... fields) {
        SSCBulkRequestBuilder bulkRequest = new SSCBulkRequestBuilder();
        SSCBulkResponse response = null;
        GetRequest roleRequest = unirestInstance.get(SSCUrls.ROLE(roleNameOrId));

        // TODO: Need to find a better way to build the request URL. But using the #queryString method incorrectly
        //  encodes spaces with the "+" character. This causes the bulk request to not return data for roles.
        GetRequest rolesRequest = unirestInstance.get(
                String.format("%s?q=name:%s&limit=2",
                        SSCUrls.ROLES,
                        roleNameOrId.replace(" ", "%20")
                )
        );

        if ( fields!=null && fields.length>0 ) {
            roleRequest.queryString("fields", String.join(",", fields));
            rolesRequest.queryString("fields", String.join(",", fields));
        }

        bulkRequest.request("role", roleRequest);
        bulkRequest.request("roles", rolesRequest);

        try{
            response = bulkRequest.execute(unirestInstance);
        }catch (UnexpectedHttpResponseException | NullPointerException e){
            throw new ValidationException("Unable to find the specified role: " + roleNameOrId);
        }

        JsonNode role = response.body("role").get("data");
        if(role != null){
            if(role.get("id") != null){
                return JsonHelper.treeToValue(role, SSCRoleDescriptor.class);
            }
        }

        JsonNode roles = response.body("roles").get("data");
        if (roles == null){
            throw new ValidationException("No role found for the role name or id: " + roleNameOrId);
        } else if( roles.size()==0 ) {
            throw new ValidationException("No role found for the role name or id: " + roleNameOrId);
        } else if ( roles.size()>1 ) {
            throw new ValidationException("Multiple roles found for the role name or id: " + roleNameOrId);
        }
        return JsonHelper.treeToValue(roles.get(0), SSCRoleDescriptor.class);
    }
}
