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

import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.crypto.SignatureHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Import.CMD_NAME)
public class PublicKeyImportCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Import outputHelper;
    @Option(names = {"--file", "-f"}, required = true) private Path pemFile;
    @Option(names = {"--name", "-n"}, required = true) private String name;

    @Override
    public JsonNode getJsonNode() {
        return SignatureHelper.publicKeyTrustStore().importKey(pemFile, name).asObjectNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "IMPORTED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
