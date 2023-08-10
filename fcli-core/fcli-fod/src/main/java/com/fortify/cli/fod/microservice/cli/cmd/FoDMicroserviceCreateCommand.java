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
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.microservice.cli.mixin.FoDMicroserviceByQualifiedNameResolverMixin;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceHelper;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceUpdateRequest;
import com.fortify.cli.fod.microservice.helper.FoDQualifiedMicroserviceNameDescriptor;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDMicroserviceCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDMicroserviceByQualifiedNameResolverMixin.PositionalParameter qualifiedMicroserviceNameResolver;

    @Option(names={"--skip-if-exists"})
    private boolean skipIfExists = false;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if (skipIfExists) {
            FoDMicroserviceDescriptor descriptor = qualifiedMicroserviceNameResolver.getMicroserviceDescriptor(unirest, false);
            if (descriptor != null) { return descriptor.asObjectNode().put("__action__", "SKIPPED_EXISTING"); }
        }
        FoDAppDescriptor appDescriptor = qualifiedMicroserviceNameResolver.getAppDescriptor(unirest, true);
        FoDQualifiedMicroserviceNameDescriptor qualifiedMicroserviceNameDescriptor = qualifiedMicroserviceNameResolver.getQualifiedMicroserviceNameDescriptor();
        FoDMicroserviceUpdateRequest msCreateRequest = FoDMicroserviceUpdateRequest.builder()
                .microserviceName(qualifiedMicroserviceNameDescriptor.getMicroserviceName())
                .build();
        return FoDMicroserviceHelper.createMicroservice(unirest, appDescriptor, msCreateRequest).asJsonNode();
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
