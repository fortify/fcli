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
package com.fortify.cli.fod.access_control.cli.cmd;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.access_control.cli.mixin.FoDUserGroupResolverMixin;
import com.fortify.cli.fod.access_control.helper.FoDUserGroupDescriptor;
import com.fortify.cli.fod.access_control.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.access_control.helper.FoDUserGroupUpdateRequest;
import com.fortify.cli.fod.access_control.helper.FoDUserHelper;
import com.fortify.cli.fod.app.helper.FoDAppHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "update-group") @CommandGroup("group")
@DefaultVariablePropertyName("id")
public class FoDGroupUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;

    @Mixin private FoDUserGroupResolverMixin.PositionalParameter userGroupResolver;

    @Option(names = {"--name"})
    private String newName;
    @Option(names = {"--add-all-users"})
    private Boolean addAllUsers = false;
    @Option(names = {"--remove-all-users"})
    private Boolean removeAllUsers = false;
    @Option(names = {"--add-users"}, required = false, split = ",", descriptionKey = "fcli.fod.user.user-names-or-ids")
    private ArrayList<String> addUsers;
    @Option(names = {"--remove-users"}, required = false, split = ",", descriptionKey = "fcli.fod.user.user-names-or-ids")
    private ArrayList<String> removeUsers;
    @Option(names = {"--add-apps", "--add-applications"}, required = false, split = ",", descriptionKey = "fcli.fod.app.app-names-or-ids")
    private ArrayList<String> addApplications;
    @Option(names = {"--remove-apps", "--remove-applications"}, required = false, split = ",", descriptionKey = "fcli.fod.app.app-names-or-ids")
    private ArrayList<String> removeApplications;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();

        FoDUserGroupDescriptor userGroupDescriptor = userGroupResolver.getUserGroupDescriptor(unirest);
        FoDUserGroupUpdateRequest userGroupUpdateRequest = FoDUserGroupUpdateRequest.builder()
                .name(StringUtils.isNotBlank(newName) ? newName : userGroupDescriptor.getName())
                .addAllUsers(addAllUsers)
                .removeAllUsers(removeAllUsers).build();

        if (addUsers != null && addUsers.size() > 0) {
            userGroupUpdateRequest.setAddUsers(FoDUserHelper.getUsersNode(unirest, addUsers));
        }
        if (removeUsers != null && removeUsers.size() > 0) {
            userGroupUpdateRequest.setRemoveUsers(FoDUserHelper.getUsersNode(unirest, removeUsers));
        }
        if (addApplications != null && addApplications.size() > 0) {
            userGroupUpdateRequest.setAddApplications(FoDAppHelper.getApplicationsNode(unirest, addApplications));
        }
        if (removeApplications != null && removeApplications.size() > 0) {
            userGroupUpdateRequest.setRemoveApplications(FoDAppHelper.getApplicationsNode(unirest, removeApplications));
        }

        return FoDUserGroupHelper.updateUserGroup(unirest, userGroupDescriptor.getId(), userGroupUpdateRequest).asJsonNode();
    }

    private void validate() {

    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDUserGroupHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

}
