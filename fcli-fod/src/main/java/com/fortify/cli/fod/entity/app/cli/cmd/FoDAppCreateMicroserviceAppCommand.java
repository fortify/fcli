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
package com.fortify.cli.fod.entity.app.cli.cmd;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.entity.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.entity.app.helper.FoDAppHelper;
import com.fortify.cli.fod.entity.attribute.helper.FoDAttributeHelper;
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

        FoDAppCreateRequest appCreateRequest = new FoDAppCreateRequest()
                .setApplicationName(appRelName.getAppName())
                .setApplicationDescription(description)
                .setBusinessCriticalityType(String.valueOf(criticalityType.getCriticalityType()))
                .setEmailList(FoDAppHelper.getEmailList(notifications))
                .setReleaseName(appRelName.getRelName())
                .setReleaseDescription(releaseDescription)
                .setSdlcStatusType(String.valueOf(sdlcStatus.getSdlcStatusType()))
                .setOwnerId(userDescriptor.getUserId())
                .setApplicationType(FoDAppTypeOptions.FoDAppType.Microservice.getName())
                .setHasMicroservices(true)
                .setMicroservices(FoDAppHelper.getMicroservicesNode(microservices))
                .setReleaseMicroserviceName(releaseMicroservice)
                .setAttributes(FoDAttributeHelper.getAttributesNode(unirest, appAttrs.getAttributes()))
                .setUserGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups));

        return FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
    }

}
