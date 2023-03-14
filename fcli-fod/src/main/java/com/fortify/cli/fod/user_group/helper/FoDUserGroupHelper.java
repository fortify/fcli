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
package com.fortify.cli.fod.user_group.helper;

import java.util.ArrayList;

import javax.validation.ValidationException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import com.fortify.cli.fod.util.FoDEnums;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

// Methods are relatively long, consider splitting
public class FoDUserGroupHelper {
    @Getter
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDUserGroupDescriptor getUserGroupDescriptor(UnirestInstance unirestInstance, String userGroupNameOrId, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.USER_GROUPS);
        try {
            int attrId = Integer.parseInt(userGroupNameOrId);
            request = request.queryString("filters", String.format("id:%d", attrId));
        } catch (NumberFormatException nfe) {
            request = request.queryString("filters", String.format("name:%s", userGroupNameOrId));
        }
        JsonNode attr = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && attr.size() == 0) {
            throw new ValidationException("No user group found for name or id: " + userGroupNameOrId);
        } else if (attr.size() > 1) {
            throw new ValidationException("Multiple user groups found for name or id: " + userGroupNameOrId);
        }
        return attr.size() == 0 ? null : JsonHelper.treeToValue(attr.get(0), FoDUserGroupDescriptor.class);
    }

    public static final FoDUserGroupDescriptor getUserGroupDescriptorById(UnirestInstance unirest, String groupId, boolean failIfNotFound) {
        GetRequest request = unirest.get(FoDUrls.USER_GROUP).routeParam("groupId", groupId);
        JsonNode group = request.asObject(ObjectNode.class).getBody();
        if (failIfNotFound && group.get("name").asText().isEmpty()) {
            throw new ValidationException("No user group for id: " + groupId);
        }
        return getDescriptor(group);
    }

    public static final FoDUserGroupDescriptor createUserGroup(UnirestInstance unirest, FoDUserGroupCreateRequest userGroupCreateRequest) {
        ObjectNode body = objectMapper.valueToTree(userGroupCreateRequest);
        JsonNode response = unirest.post(FoDUrls.USER_GROUPS)
                .body(body).asObject(JsonNode.class).getBody();
        FoDUserGroupDescriptor descriptor = JsonHelper.treeToValue(response, FoDUserGroupDescriptor.class);
        String groupId = String.valueOf(descriptor.getId());
        // add any users specified to group
        ArrayList<Integer> userstoUpdate = objectMapper.convertValue(userGroupCreateRequest.getUsers(), new TypeReference<ArrayList<Integer>>(){});
        if (userGroupCreateRequest.getUsers() != null && userGroupCreateRequest.getUsers().size() > 0) {
            for (Integer userId: userstoUpdate) {
                FoDUserGroupHelper.updateUserGroupMembership(unirest, String.valueOf(userId), groupId, FoDEnums.UserGroupMembershipAction.Add);
            }
        }
        // add group to any applications specified
        ArrayList<Integer> appsToUpdate = objectMapper.convertValue(userGroupCreateRequest.getApplications(), new TypeReference<ArrayList<Integer>>() {});
        if (userGroupCreateRequest.getApplications() != null && userGroupCreateRequest.getApplications().size() > 0) {
            for (Integer appId: appsToUpdate) {
                FoDUserGroupHelper.updateUserGroupApplicationAccess(unirest, String.valueOf(groupId), String.valueOf(appId), FoDEnums.UserGroupApplicationAccessAction.Add);
            }
        }
        return FoDUserGroupHelper.getUserGroupDescriptor(unirest, groupId, true);
    }

    public static final FoDUserGroupDescriptor updateUserGroup(UnirestInstance unirest, Integer groupId, FoDUserGroupUpdateRequest userGroupUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(userGroupUpdateRequest);
        unirest.put(FoDUrls.USER_GROUP)
                .routeParam("groupId", String.valueOf(groupId))
                .body(body).asObject(JsonNode.class).getBody();
        // add any users specified to group
        ArrayList<Integer> userstoUpdate = objectMapper.convertValue(userGroupUpdateRequest.getAddUsers(), new TypeReference<ArrayList<Integer>>() {});
        if (userGroupUpdateRequest.getAddUsers() != null && userGroupUpdateRequest.getAddUsers().size() > 0) {
            for (Integer userId: userstoUpdate) {
                FoDUserGroupHelper.updateUserGroupMembership(unirest, String.valueOf(userId), String.valueOf(groupId), FoDEnums.UserGroupMembershipAction.Add);
            }
        }
        // remove any users specified from group
        userstoUpdate = objectMapper.convertValue(userGroupUpdateRequest.getRemoveUsers(), new TypeReference<ArrayList<Integer>>() {});
        if (userGroupUpdateRequest.getRemoveUsers() != null && userGroupUpdateRequest.getRemoveUsers().size() > 0) {
            for (Integer userId: userstoUpdate) {
                FoDUserGroupHelper.updateUserGroupMembership(unirest, String.valueOf(userId), String.valueOf(groupId), FoDEnums.UserGroupMembershipAction.Remove);
            }
        }
        // add group to any applications specified
        ArrayList<Integer> appsToUpdate = objectMapper.convertValue(userGroupUpdateRequest.getAddApplications(), new TypeReference<ArrayList<Integer>>() {});
        if (userGroupUpdateRequest.getAddApplications() != null && userGroupUpdateRequest.getAddApplications().size() > 0) {
            for (Integer appId: appsToUpdate) {
                FoDUserGroupHelper.updateUserGroupApplicationAccess(unirest, String.valueOf(groupId), String.valueOf(appId), FoDEnums.UserGroupApplicationAccessAction.Add);
            }
        }
        // remove group from any applications specified
        appsToUpdate = objectMapper.convertValue(userGroupUpdateRequest.getRemoveApplications(), new TypeReference<ArrayList<Integer>>() {});
        if (userGroupUpdateRequest.getRemoveApplications() != null && userGroupUpdateRequest.getRemoveApplications().size() > 0) {
            for (Integer appId: appsToUpdate) {
                FoDUserGroupHelper.updateUserGroupApplicationAccess(unirest, String.valueOf(groupId), String.valueOf(appId), FoDEnums.UserGroupApplicationAccessAction.Remove);
            }
        }
        return getUserGroupDescriptor(unirest, String.valueOf(groupId), true);
    }

    public static final FoDUserGroupDescriptor updateUserGroupMembership(UnirestInstance unirest, String userNameOrId, String userGroupNameOrId,
                                                                         FoDEnums.UserGroupMembershipAction action) {
        FoDUserDescriptor userDescriptor = FoDUserHelper.getUserDescriptor(unirest, userNameOrId, true);
        FoDUserGroupDescriptor userGroupDescriptor = FoDUserGroupHelper.getUserGroupDescriptor(unirest, userGroupNameOrId, true);
        ArrayList<Integer> userIds = new ArrayList<>();
        userIds.add(userDescriptor.getUserId());
        FoDUserGroupMembersRequest userGroupMembersRequest = new FoDUserGroupMembersRequest();
        if (action.equals(FoDEnums.UserGroupMembershipAction.Add)) {
            userGroupMembersRequest.setRemoveUsers(new ArrayList<>());
            userGroupMembersRequest.setAddUsers(userIds);
        } else if (action.equals(FoDEnums.UserGroupMembershipAction.Remove)) {
            userGroupMembersRequest.setRemoveUsers(userIds);
            userGroupMembersRequest.setAddUsers(new ArrayList<>());
        } else {
            throw new ValidationException("Invalid action specified when updating users group membership");
        }
        ObjectNode body = objectMapper.valueToTree(userGroupMembersRequest);
        unirest.patch(FoDUrls.USER_GROUP_MEMBERS).routeParam("groupId", String.valueOf(userGroupDescriptor.getId()))
                .body(body).asObject(JsonNode.class).getBody();
        return getUserGroupDescriptor(unirest, String.valueOf(userGroupDescriptor.getId()), true);
    }

    public static final FoDUserGroupDescriptor updateUserGroupApplicationAccess(UnirestInstance unirest, String userGroupOrId, String appNameOrId,
                                                                      FoDEnums.UserGroupApplicationAccessAction action) {
        FoDUserGroupDescriptor userGroupDescriptor = FoDUserGroupHelper.getUserGroupDescriptor(unirest, userGroupOrId, true);
        FoDAppDescriptor appDescriptor = FoDAppHelper.getAppDescriptor(unirest, appNameOrId, true);
        if (action.equals(FoDEnums.UserGroupApplicationAccessAction.Add)) {
            FoDUserGroupAppAccessRequest appAccessRequest = new FoDUserGroupAppAccessRequest()
                    .setApplicationId(appDescriptor.getApplicationId());
            ObjectNode body = objectMapper.valueToTree(appAccessRequest);
            unirest.post(FoDUrls.USER_GROUP_APPLICATION_ACCESS).routeParam("userGroupId", String.valueOf(userGroupDescriptor.getId()))
                    .body(body).asEmpty();
        } else if (action.equals(FoDEnums.UserGroupApplicationAccessAction.Remove)) {
            unirest.delete(FoDUrls.USER_GROUP_APPLICATION_ACCESS_DELETE)
                    .routeParam("userGroupId", String.valueOf(userGroupDescriptor.getId()))
                    .routeParam("applicationId", String.valueOf(appDescriptor.getApplicationId()))
                    .asEmpty();
        } else {
            throw new ValidationException("Invalid action specified when updating user group application access");
        }
        return userGroupDescriptor;
    }

    public static JsonNode getUserGroupsNode(ArrayList<Integer> userGroups) {
        ArrayNode userGroupArray = objectMapper.createArrayNode();
        if (userGroups == null || userGroups.isEmpty()) return userGroupArray;
        for (Integer ug : userGroups) {
            userGroupArray.add(ug);
        }
        return userGroupArray;
    }

    public static JsonNode getUserGroupsNode(UnirestInstance unirest, ArrayList<String> userGroups) {
        ArrayNode userGroupArray = getObjectMapper().createArrayNode();
        if (userGroups == null || userGroups.isEmpty()) return userGroupArray;
        for (String ug : userGroups) {
            FoDUserGroupDescriptor userGroupDescriptor = FoDUserGroupHelper.getUserGroupDescriptor(unirest, ug, true);
            userGroupArray.add(userGroupDescriptor.getId());
        }
        return userGroupArray;
    }

    private static final FoDUserGroupDescriptor getDescriptor(JsonNode node) {
        return  JsonHelper.treeToValue(node, FoDUserGroupDescriptor.class);
    }
}
