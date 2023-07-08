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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod.entity.app.attr.cli.helper.FoDAttributeHelper;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.entity.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.entity.app.helper.FoDAppHelper;
import com.fortify.cli.fod.entity.release.cli.mixin.FoDAppAndRelNameDescriptor;
import com.fortify.cli.fod.entity.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.entity.user.helper.FoDUserHelper;
import com.fortify.cli.fod.entity.user_group.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.CreateWebApp.CMD_NAME)
public class FoDAppCreateWebAppCommand extends FoDAppCreateAppCommand {
    @Getter @Mixin private FoDOutputHelperMixins.CreateWebApp outputHelper;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
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
                .setAutoReqdAttrs(autoRequiredAttrs)
                .setAttributes(FoDAttributeHelper.getAttributesNode(unirest, appAttrs.getAttributes(), autoRequiredAttrs))
                .setUserGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups));

        return FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
    }

}
