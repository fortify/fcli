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
package com.fortify.cli.fod.entity.user_group.cli.cmd;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod.entity.app.helper.FoDAppHelper;
import com.fortify.cli.fod.entity.user.helper.FoDUserHelper;
import com.fortify.cli.fod.entity.user_group.helper.FoDUserGroupCreateRequest;
import com.fortify.cli.fod.entity.user_group.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDJsonNodeOutputCommand;

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
    @Option(names = {"--users"}, required = false, split = ",")
    private ArrayList<String> users;
    @Option(names = {"--applications"}, required = false, split = ",")
    private ArrayList<String> applications;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();

        FoDUserGroupCreateRequest userGroupCreateRequest = new FoDUserGroupCreateRequest()
                .setName(groupName)
                .setAddAllUsers(addAllUsers)
                .setApplications(FoDAppHelper.getApplicationsNode(unirest, applications))
                .setUsers(FoDUserHelper.getUsersNode(unirest, users));

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
