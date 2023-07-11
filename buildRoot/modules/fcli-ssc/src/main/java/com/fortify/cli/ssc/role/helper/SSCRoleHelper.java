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
package com.fortify.cli.ssc.role.helper;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;

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
