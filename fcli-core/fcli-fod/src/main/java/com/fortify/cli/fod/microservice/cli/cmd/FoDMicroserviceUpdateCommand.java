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
import com.fortify.cli.fod.microservice.cli.mixin.FoDMicroserviceResolverMixin;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceUpdateRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class FoDMicroserviceUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FoDMicroserviceResolverMixin.PositionalParameter appMicroserviceResolver;

    @Option(names = {"--name", "-n"}, required = true)
    private String microserviceName;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDAppMicroserviceDescriptor appMicroserviceDescriptor = appMicroserviceResolver.getAppMicroserviceDescriptor(unirest);
        FoDAppMicroserviceUpdateRequest msUpdateRequest = FoDAppMicroserviceUpdateRequest.builder()
                .microserviceName(microserviceName).build();
        return FoDAppMicroserviceHelper.updateAppMicroservice(unirest, appMicroserviceDescriptor, msUpdateRequest);
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppMicroserviceHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
