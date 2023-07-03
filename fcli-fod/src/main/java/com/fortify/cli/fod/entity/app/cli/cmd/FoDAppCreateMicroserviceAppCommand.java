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

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.entity.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.entity.app.helper.FoDAppHelper;
import com.fortify.cli.fod.entity.app.attr.cli.helper.FoDAttributeHelper;
import com.fortify.cli.fod.entity.release.cli.mixin.FoDAppAndRelNameDescriptor;
import com.fortify.cli.fod.entity.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.entity.user.helper.FoDUserHelper;
import com.fortify.cli.fod.entity.user_group.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.CreateMicroserviceApp.CMD_NAME)
public class FoDAppCreateMicroserviceAppCommand extends FoDAppCreateAppCommand {
    @Getter @Mixin private FoDOutputHelperMixins.CreateMicroserviceApp outputHelper;
    @Option(names = {"--microservices"}, required = true, split=",")
    private ArrayList<String> microservices;
    @Option(names = {"--release-microservice"}, required = true)
    private String releaseMicroservice;

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
                .applicationType(FoDAppTypeOptions.FoDAppType.Microservice.getName())
                .hasMicroservices(true)
                .autoRequiredAttrs(autoRequiredAttrs)
                .microservices(FoDAppHelper.getMicroservicesNode(microservices))
                .releaseMicroserviceName(releaseMicroservice)
                .attributes(FoDAttributeHelper.getAttributesNode(unirest, appAttrs.getAttributes(), autoRequiredAttrs))
                .userGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups)).build();

        return FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
    }

}
