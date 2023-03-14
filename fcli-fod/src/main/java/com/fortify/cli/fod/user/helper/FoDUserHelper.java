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
package com.fortify.cli.fod.user.helper;

import java.util.ArrayList;

import javax.validation.ValidationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.lookup.cli.mixin.FoDLookupTypeOptions;
import com.fortify.cli.fod.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.util.FoDEnums;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

// TODO Methods are relatively long, consider splitting if possible
public class FoDUserHelper {
    @Getter
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDUserDescriptor getUserDescriptor(UnirestInstance unirestInstance, String userNameOrId, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.USERS);
        try {
            int attrId = Integer.parseInt(userNameOrId);
            request = request.queryString("filters", String.format("userId:%d", attrId));
        } catch (NumberFormatException nfe) {
            request = request.queryString("filters", String.format("userName:%s", userNameOrId));
        }
        JsonNode attr = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && attr.size() == 0) {
            throw new ValidationException("No user found for name or id: " + userNameOrId);
        } else if (attr.size() > 1) {
            throw new ValidationException("Multiple users found for name or id: " + userNameOrId);
        }
        return attr.size() == 0 ? null : JsonHelper.treeToValue(attr.get(0), FoDUserDescriptor.class);
    }

    public static final FoDUserDescriptor getUserDescriptorById(UnirestInstance unirest, String userId, boolean failIfNotFound) {
        GetRequest request = unirest.get(FoDUrls.USER).routeParam("userId", userId);
        JsonNode user = request.asObject(ObjectNode.class).getBody();
        if (failIfNotFound && user.get("userName").asText().isEmpty()) {
            throw new ValidationException("No user found for id: " + userId);
        }
        return getDescriptor(user);
    }

    public static final FoDUserDescriptor createUser(UnirestInstance unirest, FoDUserCreateRequest userCreateRequest) {
        ObjectNode body = objectMapper.valueToTree(userCreateRequest);
        JsonNode response = unirest.post(FoDUrls.USERS)
                .body(body).asObject(JsonNode.class).getBody();
        FoDUserDescriptor descriptor = JsonHelper.treeToValue(response, FoDUserDescriptor.class);
        descriptor.asObjectNode()
                .put("userName", userCreateRequest.getUserName())
                .put("firstName", userCreateRequest.getFirstName())
                .put("lastName", userCreateRequest.getLastName())
                .put("email", userCreateRequest.getEmail());
        // add user to any groups specified
        ArrayList<Integer> groupsToUpdate = objectMapper.convertValue(userCreateRequest.getUserGroupIds(), new TypeReference<ArrayList<Integer>>() {});
        if (userCreateRequest.getUserGroupIds() != null && userCreateRequest.getUserGroupIds().size() > 0) {
            for (Integer groupId: groupsToUpdate) {
                FoDUserGroupHelper.updateUserGroupMembership(unirest, userCreateRequest.getUserName(), String.valueOf(groupId), FoDEnums.UserGroupMembershipAction.Add);
            }
        }
        // add user to any applications specified
        ArrayList<Integer> appsToUpdate = objectMapper.convertValue(userCreateRequest.getApplicationIds(), new TypeReference<ArrayList<Integer>>() {});
        if (userCreateRequest.getApplicationIds() != null && userCreateRequest.getApplicationIds().size() > 0) {
            for (Integer appId: appsToUpdate) {
                FoDUserHelper.updateUserApplicationAccess(unirest, userCreateRequest.getUserName(), String.valueOf(appId), FoDEnums.UserApplicationAccessAction.Add);
            }
        }
        return descriptor;
    }

    public static final FoDUserDescriptor updateUser(UnirestInstance unirest, Integer userId,
                                                     FoDUserUpdateRequest userUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(userUpdateRequest);
        FoDUserDescriptor userDescriptor = getUserDescriptor(unirest, String.valueOf(userId), true);
        unirest.put(FoDUrls.USER)
                .routeParam("userId", String.valueOf(userId))
                .body(body).asObject(JsonNode.class).getBody();
        // add user to any groups specified
        ArrayList<Integer> groupsToUpdate = objectMapper.convertValue(userUpdateRequest.getAddUserGroups(), new TypeReference<ArrayList<Integer>>() {});
        if (userUpdateRequest.getAddUserGroups() != null && userUpdateRequest.getAddUserGroups().size() > 0) {
            for (Integer groupId: groupsToUpdate) {
                FoDUserGroupHelper.updateUserGroupMembership(unirest, userDescriptor.getUserName(), String.valueOf(groupId), FoDEnums.UserGroupMembershipAction.Add);
            }
        }
        // remove user from any groups specified
        groupsToUpdate = objectMapper.convertValue(userUpdateRequest.getRemoveUserGroups(), new TypeReference<ArrayList<Integer>>() {});
        if (userUpdateRequest.getRemoveUserGroups() != null && userUpdateRequest.getRemoveUserGroups().size() > 0) {
            for (Integer groupId: groupsToUpdate) {
                FoDUserGroupHelper.updateUserGroupMembership(unirest, userDescriptor.getUserName(), String.valueOf(groupId), FoDEnums.UserGroupMembershipAction.Remove);
            }
        }
        // add user to any applications specified
        ArrayList<Integer> appsToUpdate = objectMapper.convertValue(userUpdateRequest.getAddApplications(), new TypeReference<ArrayList<Integer>>() {});
        if (userUpdateRequest.getAddApplications() != null && userUpdateRequest.getAddApplications().size() > 0) {
            for (Integer appId: appsToUpdate) {
                FoDUserHelper.updateUserApplicationAccess(unirest, String.valueOf(userId), String.valueOf(appId), FoDEnums.UserApplicationAccessAction.Add);
            }
        }
        // remove user from any applications specified
        appsToUpdate = objectMapper.convertValue(userUpdateRequest.getRemoveApplications(), new TypeReference<ArrayList<Integer>>() {});
        if (userUpdateRequest.getRemoveApplications() != null && userUpdateRequest.getRemoveApplications().size() > 0) {
            for (Integer appId: appsToUpdate) {
                FoDUserHelper.updateUserApplicationAccess(unirest, String.valueOf(userId), String.valueOf(appId), FoDEnums.UserApplicationAccessAction.Remove);
            }
        }
        return getUserDescriptorById(unirest, String.valueOf(userId), true);
    }

    public static final FoDUserDescriptor updateUserApplicationAccess(UnirestInstance unirest, String userNameOrId, String appNameOrId,
                                                                      FoDEnums.UserApplicationAccessAction action) {
        FoDUserDescriptor userDescriptor = FoDUserHelper.getUserDescriptor(unirest, userNameOrId, true);
        FoDAppDescriptor appDescriptor = FoDAppHelper.getAppDescriptor(unirest, appNameOrId, true);
        if (action.equals(FoDEnums.UserApplicationAccessAction.Add)) {
            FoDUserAppAccessRequest appAccessRequest = new FoDUserAppAccessRequest()
                    .setApplicationId(appDescriptor.getApplicationId());
            ObjectNode body = objectMapper.valueToTree(appAccessRequest);
            unirest.post(FoDUrls.USER_APPLICATION_ACCESS).routeParam("userId", String.valueOf(userDescriptor.getUserId()))
                    .body(body).asEmpty();
        } else if (action.equals(FoDEnums.UserApplicationAccessAction.Remove)) {
            unirest.delete(FoDUrls.USER_APPLICATION_ACCESS_DELETE)
                    .routeParam("userId", String.valueOf(userDescriptor.getUserId()))
                    .routeParam("applicationId", String.valueOf(appDescriptor.getApplicationId()))
                    .asEmpty();
        } else {
            throw new ValidationException("Invalid action specified when updating users application access");
        }
        return userDescriptor;
    }

    public static Integer getRoleId(UnirestInstance unirest, String roleNameOrId) {
        int roleId = 0;
        try {
            roleId = Integer.parseInt(roleNameOrId);
        } catch (NumberFormatException nfe) {
            try {
                FoDLookupDescriptor lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupTypeOptions.FoDLookupType.Roles, roleNameOrId, true);
                roleId = Integer.valueOf(lookupDescriptor.getValue());
            } catch (JsonProcessingException e) {
                throw new ValidationException("Unable to find role with name: " + roleNameOrId);
            }
        }
        return roleId;
    }

    public static JsonNode getUsersNode(UnirestInstance unirest, ArrayList<String> users) {
        ArrayNode userArray = getObjectMapper().createArrayNode();
        if (users == null || users.isEmpty()) return userArray;
        for (String u : users) {
            FoDUserDescriptor userDescriptor = FoDUserHelper.getUserDescriptor(unirest, u, true);
            userArray.add(userDescriptor.getUserId());
        }
        return userArray;
    }

    private static final FoDUserDescriptor getDescriptor(JsonNode node) {
        return  JsonHelper.treeToValue(node, FoDUserDescriptor.class);
    }
}
