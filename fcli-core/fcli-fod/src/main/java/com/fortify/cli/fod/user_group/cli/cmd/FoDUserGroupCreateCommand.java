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
package com.fortify.cli.fod.user_group.cli.cmd;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupCreateRequest;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDUserGroupCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;

    @Parameters(index = "0", descriptionKey = "group-name")
    private String groupName;
    @Option(names = {"--add-all-users"})
    private Boolean addAllUsers = false;
    @Option(names = {"--users"}, required = false, split = ",", descriptionKey = "fcli.fod.user.user-name-or-id")
    private ArrayList<String> users;
    @Option(names = {"--applications"}, required = false, split = ",", descriptionKey = "fcli.fod.app-name-or-id")
    private ArrayList<String> applications;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();

        FoDUserGroupCreateRequest userGroupCreateRequest = FoDUserGroupCreateRequest.builder()
                .name(groupName)
                .addAllUsers(addAllUsers)
                .applications(FoDAppHelper.getApplicationsNode(unirest, applications))
                .users(FoDUserHelper.getUsersNode(unirest, users)).build();

        return FoDUserGroupHelper.createUserGroup(unirest, userGroupCreateRequest).asJsonNode();
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
