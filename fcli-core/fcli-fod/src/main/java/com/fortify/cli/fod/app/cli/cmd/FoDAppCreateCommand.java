/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod.app.cli.cmd;

import static com.fortify.cli.common.util.DisableTest.TestType.MULTI_OPT_PLURAL_NAME;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDMicroserviceAndReleaseNameResolverMixin;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateOptions;

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
    //private static final Logger LOG = LoggerFactory.getLogger(FoDAppCreateCommand.class);
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Spec CommandSpec spec;

    @EnvSuffix("NAME") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.fod.app.app-name")
    protected String applicationName;

    @Option(names = {"--description", "-d"})
    protected String description;
    @Option(names={"--skip-if-exists"})
    private boolean skipIfExists = false;
    @DisableTest(MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--notify"}, required = false, split=",")
    protected ArrayList<String> notifications;
    @Mixin private FoDMicroserviceAndReleaseNameResolverMixin.RequiredOption releaseNameResolverMixin;
    @Option(names = {"--release-description"})
    protected String releaseDescription;
    @Option(names = {"--owner"}, required = false)
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
    protected FoDAttributeUpdateOptions.OptionalAttrCreateOption appAttrs;
    @Mixin
    protected FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        boolean msCreated = false;
        boolean relCreated = false;
        if (skipIfExists) {
            var descriptor = FoDAppHelper.getAppDescriptor(unirest, applicationName, false);
            if (descriptor != null) {
                return addActionCommandResult(descriptor.asObjectNode(), false, false, false);
            }
        }
        var releaseNameDescriptor = releaseNameResolverMixin.getMicroserviceAndReleaseNameDescriptor();
        var microserviceName = releaseNameDescriptor.getMicroserviceName();
        var releaseName = releaseNameDescriptor.getMicroserviceName();
        validateMicroserviceName(microserviceName);
        FoDAppCreateRequest appCreateRequest = FoDAppCreateRequest.builder()
                .applicationName(applicationName)
                .applicationDescription(description)
                .businessCriticality(criticalityType.getCriticalityType())
                .notify(notifications)
                .microserviceAndReleaseNameDescriptor(releaseNameDescriptor)
                .releaseDescription(releaseDescription)
                .sdlcStatus(sdlcStatus.getSdlcStatusType())
                .owner(unirest, owner)
                .appType(appType.getAppType())
                .autoAttributes(unirest, appAttrs.getAttributes(), autoRequiredAttrs)
                .userGroups(unirest, userGroups)
                .build().validate();
        msCreated = (microserviceName != null && StringUtils.isNotBlank(microserviceName));
        relCreated = (releaseName != null && StringUtils.isNotBlank(releaseName));
        var app = FoDAppHelper.createApp(unirest, appCreateRequest);
        return addActionCommandResult(app.asObjectNode(), true, msCreated, relCreated);
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

    private final ObjectNode addActionCommandResult(ObjectNode rel, boolean appCreated, boolean msCreated, boolean relCreated) {
        var result = new ArrayList<String>();
        addActionCommandResult(result, appCreated, "APP_CREATED");
        addActionCommandResult(result, msCreated,  "MICROSERVICE_CREATED");
        addActionCommandResult(result, relCreated, "RELEASE_CREATED");
        if ( result.isEmpty() ) { result.add("SKIPPED_EXISTING"); }
        return rel.put(IActionCommandResultSupplier.actionFieldName, String.join("\n", result));
    }
    
    private final void addActionCommandResult(ArrayList<String> result, boolean add, String value) {
        if ( add ) { result.add(value); }
    }

}
