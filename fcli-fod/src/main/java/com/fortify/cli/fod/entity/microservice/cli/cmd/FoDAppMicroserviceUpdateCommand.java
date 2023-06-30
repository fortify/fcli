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

package com.fortify.cli.fod.entity.microservice.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod.entity.microservice.cli.mixin.FoDAppMicroserviceResolverMixin;
import com.fortify.cli.fod.entity.microservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.entity.microservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.entity.microservice.helper.FoDAppMicroserviceUpdateRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class FoDAppMicroserviceUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FoDAppMicroserviceResolverMixin.PositionalParameter appMicroserviceResolver;

    @Option(names = {"--name", "-n"}, required = true)
    private String microserviceName;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDAppMicroserviceDescriptor appMicroserviceDescriptor = appMicroserviceResolver.getAppMicroserviceDescriptor(unirest);
        FoDAppMicroserviceUpdateRequest msUpdateRequest = new FoDAppMicroserviceUpdateRequest()
                .setMicroserviceName(microserviceName);
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
