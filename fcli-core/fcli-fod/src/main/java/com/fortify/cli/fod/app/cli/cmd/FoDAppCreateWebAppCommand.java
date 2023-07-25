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
package com.fortify.cli.fod.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.app.attr.cli.helper.FoDAttributeHelper;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDAppAndRelNameDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;

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

        FoDAppCreateRequest appCreateRequest = FoDAppCreateRequest.builder()
                .applicationName(appRelName.getAppName())
                .applicationDescription(description)
                .businessCriticalityType(String.valueOf(criticalityType.getCriticalityType()))
                .emailList(FoDAppHelper.getEmailList(notifications))
                .releaseName(appRelName.getRelName())
                .releaseDescription(releaseDescription)
                .sdlcStatusType(String.valueOf(sdlcStatus.getSdlcStatusType()))
                .ownerId(userDescriptor.getUserId())
                .applicationType(FoDAppTypeOptions.FoDAppType.Web.getName())
                .hasMicroservices(false)
                .autoRequiredAttrs(autoRequiredAttrs)
                .attributes(FoDAttributeHelper.getAttributesNode(unirest, appAttrs.getAttributes(), autoRequiredAttrs))
                .userGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups)).build();

        return FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
    }

}