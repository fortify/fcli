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
import com.fortify.cli.fod.microservice.cli.mixin.FoDAppMicroserviceResolverMixin;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class FoDAppMicroserviceDeleteCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Spec CommandSpec spec;

    @Mixin private FoDAppMicroserviceResolverMixin.PositionalParameter appMicroserviceResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDAppMicroserviceDescriptor appMicroserviceDescriptor = appMicroserviceResolver.getAppMicroserviceDescriptor(unirest);
        return FoDAppMicroserviceHelper.deleteAppMicroservice(unirest, appMicroserviceDescriptor);
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppMicroserviceHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
