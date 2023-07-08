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
package com.fortify.cli.fod.entity.app.cli.cmd;

import static com.fortify.cli.common.util.DisableTest.TestType.MULTI_OPT_PLURAL_NAME;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.fod.entity.app.attr.cli.helper.FoDAttributeHelper;
import com.fortify.cli.fod.entity.app.attr.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.entity.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.entity.app.helper.FoDAppHelper;
import com.fortify.cli.fod.entity.release.cli.mixin.FoDAppAndRelNameDescriptor;
import com.fortify.cli.fod.entity.release.cli.mixin.FoDAppRelResolverMixin;
import com.fortify.cli.fod.entity.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.entity.user.helper.FoDUserHelper;
import com.fortify.cli.fod.entity.user_group.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

public abstract class FoDAppCreateAppCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Spec CommandSpec spec;

    @Mixin protected FoDAppRelResolverMixin.PositionalParameter appRelResolver;

    @Option(names = {"--description", "-d"})
    protected String description;
    @DisableTest(MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--notify"}, required = false, split=",")
    protected ArrayList<String> notifications;
    @Option(names = {"--release-description", "--rel-desc"})
    protected String releaseDescription;
    @Option(names = {"--owner"}, required = true)
    protected String owner;
    @Option(names = {"--user-groups", "--groups"}, required = false, split=",")
    protected ArrayList<String> userGroups;
    @Option(names={"--auto-required-attrs"}, required = false)
    protected boolean autoRequiredAttrs = false;

    @Mixin
    protected FoDCriticalityTypeOptions.RequiredOption criticalityType;
    @Mixin
    protected FoDAttributeUpdateOptions.OptionalAttrOption appAttrs;
    @Mixin
    protected FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();

        FoDAppAndRelNameDescriptor appRelName = appRelResolver.getAppAndRelName();
        FoDUserDescriptor userDescriptor = FoDUserHelper.getUserDescriptor(unirest, owner, true);

        FoDAppCreateRequest appCreateRequest = new FoDAppCreateRequest()
                .setApplicationName(appRelName.getAppName())
                .setApplicationDescription(description)
                .setBusinessCriticalityType(String.valueOf(criticalityType.getCriticalityType()))
                .setEmailList(FoDAppHelper.getEmailList(notifications))
                .setReleaseName(appRelName.getRelName())
                .setReleaseDescription(releaseDescription)
                .setSdlcStatusType(String.valueOf(sdlcStatus.getSdlcStatusType()))
                .setOwnerId(userDescriptor.getUserId())
                .setApplicationType(FoDAppTypeOptions.FoDAppType.Web.getName())
                .setHasMicroservices(false)
                .setAttributes(FoDAttributeHelper.getAttributesNode(unirest, appAttrs.getAttributes(), autoRequiredAttrs))
                .setUserGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups));

        return FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
    }

    protected void validate() {
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppHelper.renameFields(record);
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
