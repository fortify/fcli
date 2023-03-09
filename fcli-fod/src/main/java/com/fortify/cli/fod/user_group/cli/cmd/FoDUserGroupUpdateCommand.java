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
package com.fortify.cli.fod.user_group.cli.cmd;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import com.fortify.cli.fod.user_group.cli.mixin.FoDUserGroupResolverMixin;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupDescriptor;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupUpdateRequest;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Update.CMD_NAME)
public class FoDUserGroupUpdateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Create outputHelper;

    @Mixin private FoDUserGroupResolverMixin.PositionalParameter userGroupResolver;

    @Option(names = {"--new-name"})
    private String newName;
    @Option(names = {"--add-all-users"})
    private Boolean addAllUsers = false;
    @Option(names = {"--remove-all-users"})
    private Boolean removeAllUsers = false;
    @Option(names = {"--add-user"}, required = false, arity = "0..*")
    private ArrayList<String> addUsers;
    @Option(names = {"--remove-user"}, required = false, arity = "0..*")
    private ArrayList<String> removeUsers;
    @Option(names = {"--add-app", "--add-application"}, required = false, arity = "0..*")
    private ArrayList<String> addApplications;
    @Option(names = {"--remove-app", "--remove-application"}, required = false, arity = "0..*")
    private ArrayList<String> removeApplications;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();

        FoDUserGroupDescriptor userGroupDescriptor = userGroupResolver.getUserGroupDescriptor(unirest);
        FoDUserGroupUpdateRequest userGroupUpdateRequest = new FoDUserGroupUpdateRequest()
                .setName(newName != null && StringUtils.isNotEmpty(newName) ? newName : userGroupDescriptor.getName())
                .setAddAllUsers(addAllUsers)
                .setRemoveAllUsers(removeAllUsers);

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
