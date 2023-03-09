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
package com.fortify.cli.fod.user.cli.cmd;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.user.cli.mixin.FoDUserResolverMixin;
import com.fortify.cli.fod.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import com.fortify.cli.fod.user.helper.FoDUserUpdateRequest;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Update.CMD_NAME)
public class FoDUserUpdateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Update outputHelper;
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
    @Option(names = {"--add-group"}, required = false, arity = "0..*")
    private ArrayList<String> addUserGroups;
    @Option(names = {"--remove-group"}, required = false, arity = "0..*")
    private ArrayList<String> removeUserGroups;
    @Option(names = {"--add-app", "--add-application"}, required = false, arity = "0..*")
    private ArrayList<String> addApplications;
    @Option(names = {"--remove-app", "--remove-application"}, required = false, arity = "0..*")
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
