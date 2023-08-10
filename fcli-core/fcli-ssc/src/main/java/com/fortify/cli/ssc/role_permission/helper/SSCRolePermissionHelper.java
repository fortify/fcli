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
package com.fortify.cli.ssc.role_permission.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;

import kong.unirest.core.GetRequest;
import kong.unirest.core.UnirestInstance;

public class SSCRolePermissionHelper {

    // TODO: This is pretty generic. Need to find a better class to put this method in.
    public static final JsonNode flattenArrayProperty(JsonNode recordJsonNode, String propertyName, String propertyNameToFlattenOn) {
        ObjectNode record = (ObjectNode)recordJsonNode;
        String newRecord = "";
        for (JsonNode e : record.get(propertyName)) {
            newRecord += e.get(propertyNameToFlattenOn).asText() + ",";
        }
        if(newRecord.length() > 1){
            newRecord = newRecord.substring(0,newRecord.length()-1);
        }
        record.remove(propertyName);
        record.put(propertyName, newRecord);
        return record;
    }

    public static final SSCRolePermissionDescriptor getRolePermission(UnirestInstance unirestInstance, String rolePermissionNameOrId, String... fields) {
        SSCBulkRequestBuilder bulkRequest = new SSCBulkRequestBuilder();
        SSCBulkResponse response = null;
        GetRequest rolePermissionRequest = unirestInstance.get(SSCUrls.PERMISSION(rolePermissionNameOrId));

        // TODO: Need to find a better way to build the request URL. But using the #queryString method incorrectly
        //  encodes spaces with the "+" character. This causes the bulk request to not return data for roles.
        GetRequest rolePermissionsRequest = unirestInstance.get(
                String.format("%s?q=name:%s&limit=2",
                        SSCUrls.PERMISSIONS,
                        rolePermissionNameOrId.replace(" ", "%20")
                )
        );

        if ( fields!=null && fields.length>0 ) {
            rolePermissionRequest.queryString("fields", String.join(",", fields));
            rolePermissionsRequest.queryString("fields", String.join(",", fields));
        }

        bulkRequest.request("rolePermission", rolePermissionRequest);
        bulkRequest.request("rolePermissions", rolePermissionsRequest);

        try{
            response = bulkRequest.execute(unirestInstance);
        }catch (UnexpectedHttpResponseException | NullPointerException e){
            throw new IllegalArgumentException("Unable to find the specified role: " + rolePermissionNameOrId);
        }

        JsonNode rolePermission = response.body("rolePermission").get("data");
        if(rolePermission != null){
            if(rolePermission.get("id") != null){
                return JsonHelper.treeToValue(rolePermission, SSCRolePermissionDescriptor.class);
            }
        }

        JsonNode rolePermissions = response.body("rolePermissions").get("data");
        if (rolePermissions == null){
            throw new IllegalArgumentException("No role permission found for the role permission name or id: " + rolePermissionNameOrId);
        } else if( rolePermissions.size()==0 ) {
            throw new IllegalArgumentException("No role permission found for the role permission name or id: " + rolePermissionNameOrId);
        } else if ( rolePermissions.size()>1 ) {
            throw new IllegalArgumentException("Multiple role permissions found for the role permission name or id: " + rolePermissionNameOrId);
        }
        return JsonHelper.treeToValue(rolePermissions.get(0), SSCRolePermissionDescriptor.class);
    }
}
