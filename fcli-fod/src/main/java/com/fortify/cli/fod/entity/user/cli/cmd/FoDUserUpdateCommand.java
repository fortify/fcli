/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.entity.user.cli.cmd;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod.entity.app.helper.FoDAppHelper;
import com.fortify.cli.fod.entity.user.cli.mixin.FoDUserResolverMixin;
import com.fortify.cli.fod.entity.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.entity.user.helper.FoDUserHelper;
import com.fortify.cli.fod.entity.user.helper.FoDUserUpdateRequest;
import com.fortify.cli.fod.entity.user_group.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDJsonNodeOutputCommand;

import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class FoDUserUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FoDUserResolverMixin.PositionalParameter userResolver;

    @Option(names = {"--email"})
    private String email;
    @Option(names = {"--firstname"})
    private String firstName;
    @Option(names = {"--lastname"})
    private String lastName;
    @Option(names = {"--phone", "--phone-number"})
    private String phoneNumber;
    @Option(names = {"--password"})
    private String password;
    @Option(names = {"--role"})
    private String roleNameOrId;
    @Option(names = {"--password-never-expires"})
    private Boolean passwordNeverExpires = false;
    @Option(names = {"--suspended"})
    private Boolean isSuspended = false;
    @Option(names = {"--must-change"})
    private Boolean mustChange = false;
    @Option(names = {"--add-groups"}, required = false, split = ",")
    private ArrayList<String> addUserGroups;
    @Option(names = {"--remove-groups"}, required = false, split = ",")
    private ArrayList<String> removeUserGroups;
    @Option(names = {"--add-apps", "--add-applications"}, required = false, split = ",")
    private ArrayList<String> addApplications;
    @Option(names = {"--remove-apps", "--remove-applications"}, required = false, split = ",")
    private ArrayList<String> removeApplications;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();

        int roleId = 0;
        FoDUserDescriptor userDescriptor = userResolver.getUserDescriptor(unirest);
        if (roleNameOrId != null && !roleNameOrId.isEmpty()) {
            roleId = FoDUserHelper.getRoleId(unirest, roleNameOrId);
        }

        FoDUserUpdateRequest userUpdateRequest = new FoDUserUpdateRequest()
                .setEmail(email != null && StringUtils.isNotEmpty(email) ? email : userDescriptor.getUserName())
                .setFirstName(firstName != null && StringUtils.isNotEmpty(firstName) ? firstName : userDescriptor.getFirstName())
                .setLastName(lastName != null && StringUtils.isNotEmpty(lastName) ? lastName : userDescriptor.getLastName())
                .setPhoneNumber(phoneNumber != null && StringUtils.isNotEmpty(phoneNumber) ? phoneNumber : userDescriptor.getPhoneNumber())
                .setRoleId(roleId > 0 ? roleId : userDescriptor.getRoleId())
                .setPasswordNeverExpires(passwordNeverExpires)
                .setIsSuspended(isSuspended)
                .setMustChange(mustChange);

        if (password != null && StringUtils.isNotEmpty(password)) {
            userUpdateRequest.setPassword(password);
        }
        if (addUserGroups != null && addUserGroups.size() > 0) {
            userUpdateRequest.setAddUserGroups(FoDUserGroupHelper.getUserGroupsNode(unirest, addUserGroups));
        }
        if (removeUserGroups != null && removeUserGroups.size() > 0) {
            userUpdateRequest.setRemoveUserGroups(FoDUserGroupHelper.getUserGroupsNode(unirest, removeUserGroups));
        }
        if (addApplications != null && addApplications.size() > 0) {
            userUpdateRequest.setAddApplications(FoDAppHelper.getApplicationsNode(unirest, addApplications));
        }
        if (removeApplications != null && removeApplications.size() > 0) {
            userUpdateRequest.setRemoveApplications(FoDAppHelper.getApplicationsNode(unirest, removeApplications));
        }

        return FoDUserHelper.updateUser(unirest, userDescriptor.getUserId(), userUpdateRequest).asJsonNode();
    }

    private void validate() {

    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDUserHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

}
