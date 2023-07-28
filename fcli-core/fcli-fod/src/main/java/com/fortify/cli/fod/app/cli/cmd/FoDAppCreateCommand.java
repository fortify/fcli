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
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.app.attr.cli.helper.FoDAttributeHelper;
import com.fortify.cli.fod.app.attr.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.ArrayList;
import java.util.Arrays;

import static com.fortify.cli.common.util.DisableTest.TestType.MULTI_OPT_PLURAL_NAME;

@CommandLine.Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDAppCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Spec CommandSpec spec;

    @Parameters(index = "0", arity = "1", descriptionKey = "fcli.fod.app.app-name")
    private String applicationName;
    @Option(names = {"--description", "-d"})
    protected String description;
    @DisableTest(MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--notify"}, required = false, split=",")
    protected ArrayList<String> notifications;
    @Option(names = {"--release", "--release-name"})
    protected String releaseName;
    @Option(names = {"--release-description"})
    protected String releaseDescription;
    @Option(names = {"--microservice", "--microservice-name"}, required = false)
    private String microservice;
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
        validate();

        FoDUserDescriptor userDescriptor = FoDUserHelper.getUserDescriptor(unirest, owner, true);
        ArrayList<String> microservices = new ArrayList<>(Arrays.asList(microservice));
        FoDAppCreateRequest appCreateRequest = FoDAppCreateRequest.builder()
                .applicationName(applicationName)
                .applicationDescription(description)
                .businessCriticalityType(String.valueOf(criticalityType.getCriticalityType()))
                .emailList(FoDAppHelper.getEmailList(notifications))
                .releaseName(releaseName)
                .releaseDescription(releaseDescription)
                .sdlcStatusType(String.valueOf(sdlcStatus.getSdlcStatusType()))
                .ownerId(userDescriptor.getUserId())
                .applicationType(appType.getAppType().getName())
                .hasMicroservices(appType.getAppType().isMicroservice())
                .microservices(FoDAppHelper.getMicroservicesNode(microservices))
                .attributes(FoDAttributeHelper.getAttributesNode(unirest, appAttrs.getAttributes(), autoRequiredAttrs))
                .userGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups)).build();

        return FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
    }

    protected void validate() {
        if (appType.getAppType().equals(FoDAppTypeOptions.FoDAppType.Microservice)) {
            if (StringUtils.isBlank(microservice)) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        "Missing option: if 'Microservice' type is specified then the '--microservice-name' option needs to specified.");
            }
        }
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
