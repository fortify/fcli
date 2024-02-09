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
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.microservice.cli.mixin.FoDMicroserviceByQualifiedNameResolverMixin;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class FoDMicroserviceDeleteCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDMicroserviceByQualifiedNameResolverMixin.PositionalParameter qualifiedMicroserviceNameResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDMicroserviceDescriptor appMicroserviceDescriptor = qualifiedMicroserviceNameResolver.getMicroserviceDescriptor(unirest, true);
        return FoDMicroserviceHelper.deleteMicroservice(unirest, appMicroserviceDescriptor).asJsonNode();
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
