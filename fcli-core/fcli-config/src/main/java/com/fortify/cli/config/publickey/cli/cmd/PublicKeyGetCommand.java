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
package com.fortify.cli.config.publickey.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.config.publickey.cli.mixin.PublicKeyResolverMixin;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class PublicKeyGetCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin PublicKeyResolverMixin.PositionalParameter publicKeyResolver;

    @Override
    public JsonNode getJsonNode() {
        return publicKeyResolver.getPublicKeyDescriptor().asObjectNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
