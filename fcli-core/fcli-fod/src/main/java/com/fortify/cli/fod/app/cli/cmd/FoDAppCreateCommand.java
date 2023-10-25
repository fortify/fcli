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

import static com.fortify.cli.common.util.DisableTest.TestType.MULTI_OPT_PLURAL_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.access_control.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.access_control.helper.FoDUserHelper;
import com.fortify.cli.fod.app.attr.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.app.attr.helper.FoDAttributeHelper;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDMicroserviceAndReleaseNameResolverMixin;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.app.helper.FoDAppHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@CommandLine.Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDAppCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Spec CommandSpec spec;

    @EnvSuffix("NAME") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.fod.app.app-name")
    protected String applicationName;

    @Option(names = {"--description", "-d"})
    protected String description;
    @DisableTest(MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--notify"}, required = false, split=",")
    protected ArrayList<String> notifications;
    @Mixin private FoDMicroserviceAndReleaseNameResolverMixin.RequiredOption releaseNameResolverMixin;
    @Option(names = {"--release-description"})
    protected String releaseDescription;
    @Option(names = {"--owner"}, required = true)
    protected String owner;
    @Option(names = {"--groups"}, required = false, split=",")
    protected ArrayList<String> userGroups;
    @Option(names={"--auto-required-attrs"}, required = false)
    protected boolean autoRequiredAttrs = false;

    @Mixin
    protected FoDAppTypeOptions.RequiredAppTypeOption appType;
    @Mixin
    protected FoDCriticalityTypeOptions.RequiredOption criticalityType;
    @Mixin
    protected FoDAttributeUpdateOptions.OptionalAttrOption appAttrs;
    @Mixin
    protected FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseNameDescriptor = releaseNameResolverMixin.getMicroserviceAndReleaseNameDescriptor();
        var microserviceName = releaseNameDescriptor.getMicroserviceName();
        validateMicroserviceName(microserviceName);

        var ownerId = FoDUserHelper.getUserDescriptor(unirest, owner, true).getUserId();
        List<String> microservices = StringUtils.isBlank(microserviceName)
                ? Collections.emptyList() : new ArrayList<>(Arrays.asList(microserviceName));
        FoDAppCreateRequest appCreateRequest = FoDAppCreateRequest.builder()
                .applicationName(applicationName)
                .applicationDescription(description)
                .businessCriticalityType(criticalityType.getCriticalityType().name())
                .emailList(FoDAppHelper.getEmailList(notifications))
                .releaseName(releaseNameDescriptor.getReleaseName())
                .releaseDescription(releaseDescription)
                .sdlcStatusType(String.valueOf(sdlcStatus.getSdlcStatusType()))
                .ownerId(ownerId)
                .applicationType(appType.getAppType().getFoDValue())
                .hasMicroservices(appType.getAppType().isMicroservice())
                .microservices(FoDAppHelper.getMicroservicesNode(microservices))
                .attributes(FoDAttributeHelper.getAttributesNode(unirest, appAttrs.getAttributes(), autoRequiredAttrs))
                .userGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups)).build();

        return FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
    }

    protected void validateMicroserviceName(String microserviceName) {
        if ( appType.getAppType().equals(FoDAppTypeOptions.FoDAppType.Microservice) ) {
            if ( StringUtils.isBlank(microserviceName) ) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                    "Invalid option value: if 'Microservice' type is specified then --release must be specified as <microservice>:<release>");
            }
        } else if ( StringUtils.isNotBlank(microserviceName) ) {
            throw new CommandLine.ParameterException(spec.commandLine(),
               "Invalid option value: --release must be a plain release name for non-microservice applications.");
        }
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppHelper.transformRecord(record);
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
