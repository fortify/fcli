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
package com.fortify.cli.ssc.action.cli.cmd;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.crypto.SignatureHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "sign")
public class SSCActionSignCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin OutputHelperMixins.TableNoQuery outputHelper;
    @Option(names="--in") private Path in;
    @Option(names="--out") private Path out;
    @Option(names="--pem-file") private Path pemFile;
    @Option(names = {"--password", "-p"}, interactive = true, echo = false, arity = "0..1", required = false) 
    private char[] password;
    
    @Override
    public JsonNode getJsonNode() {
        var fingerprint = SignatureHelper.signer(pemFile, password).publicKeyFingerprint();
        var fortifyFingerPrint = SignatureHelper.fortifySignatureVerifier().publicKeyFingerPrint();
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("fingerprint", fingerprint)
                .put("fortifyFingerprint", fortifyFingerPrint);
        
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
