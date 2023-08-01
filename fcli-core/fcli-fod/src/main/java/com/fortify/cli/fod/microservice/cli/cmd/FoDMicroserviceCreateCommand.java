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

package com.fortify.cli.fod.microservice.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.microservice.cli.mixin.FoDAppAndMicroserviceNameDescriptor;
import com.fortify.cli.fod.microservice.cli.mixin.FoDAppAndMicroserviceNameResolverMixin;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceHelper;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceUpdateRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDMicroserviceCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private FoDAppAndMicroserviceNameResolverMixin.PositionalParameter appAndMicroserviceNameResolver;

    @Option(names={"--skip-if-exists"})
    private boolean skipIfExists = false;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if (skipIfExists) {
            FoDMicroserviceDescriptor descriptor = FoDMicroserviceHelper.getOptionalAppMicroserviceFromAppAndMicroserviceName(unirest, appAndMicroserviceNameResolver.getAppAndMicroserviceNameDescriptor());
            if (descriptor != null) { return descriptor.asObjectNode().put("__action__", "SKIPPED_EXISTING"); }
        }
        FoDAppAndMicroserviceNameDescriptor appAndMicroserviceNameDescriptor = FoDAppAndMicroserviceNameDescriptor.fromCombinedAppAndMicroserviceName(
                appAndMicroserviceNameResolver.getAppAndMicroserviceName(), appAndMicroserviceNameResolver.getDelimiter());

        FoDAppDescriptor appDescriptor = FoDAppHelper.getAppDescriptor(unirest, appAndMicroserviceNameDescriptor.getAppName(), true);
        FoDMicroserviceUpdateRequest msCreateRequest = FoDMicroserviceUpdateRequest.builder()
                .microserviceName(appAndMicroserviceNameDescriptor.getMicroserviceName()).build();
        return FoDMicroserviceHelper.createAppMicroservice(unirest, appDescriptor.getApplicationId(), msCreateRequest);
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDMicroserviceHelper.renameFields(record);
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
