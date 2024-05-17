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
import com.fortify.cli.common.cli.mixin.CommonOptionMixins.AbstractTextResolverMixin;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Import.CMD_NAME)
public class PublicKeyImportCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Import outputHelper;
    @Mixin private PublicKeyResolverMixin publicKeyResolver;
    @Option(names = {"--name", "-n"}, required = true) private String name;

    @Override
    public JsonNode getJsonNode() {
        return SignatureHelper.publicKeyTrustStore()
                .importKey(publicKeyResolver.getText(), name)
                .asObjectNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "IMPORTED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private static class PublicKeyResolverMixin extends AbstractTextResolverMixin {
        @Getter @Parameters(arity = "1", descriptionKey = "fcli.config.public-key.resolver", paramLabel = "source") private String textSource;
    }
}
