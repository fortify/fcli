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
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.access_control.helper.FoDUserCreateRequest;
import com.fortify.cli.fod.access_control.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.access_control.helper.FoDUserHelper;
import com.fortify.cli.fod.app.helper.FoDAppHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create-user") @CommandGroup("user")
@DefaultVariablePropertyName("userId")
public class FoDUserCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;

    @EnvSuffix("NAME") @Parameters(index = "0", descriptionKey = "user-name")
    private String userName;
    @Option(names = {"--email"}, required = true)
    private String email;
    @Option(names = {"--firstname"}, required = true)
    private String firstName;
    @Option(names = {"--lastname"}, required = true)
    private String lastName;
    @Option(names = {"--phone", "--phone-number"})
    private String phoneNumber;
    @Option(names = {"--role"}, required = true)
    private String roleNameOrId;
    @Option(names = {"--groups"}, required = false, split = ",", descriptionKey = "fcli.fod.group.group-names-or-ids")
    private ArrayList<String> userGroups;
    @Option(names = {"--applications"}, required = false, split=",", descriptionKey = "fcli.fod.app.app-names-or-ids")
    private ArrayList<String> applications;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();

        FoDUserCreateRequest userCreateRequest = FoDUserCreateRequest.builder()
                .userName(userName)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .roleId(FoDUserHelper.getRoleId(unirest, roleNameOrId))
                .build();

        if (userGroups != null && userGroups.size() > 0) {
            userCreateRequest.setUserGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups));
        }
        if (applications != null && applications.size() > 0) {
            userCreateRequest.setApplicationIds(FoDAppHelper.getApplicationsNode(unirest, applications));
        }

        return FoDUserHelper.createUser(unirest, userCreateRequest).asJsonNode();
    }

    private void validate() {

    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDUserHelper.renameFields(record);
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
