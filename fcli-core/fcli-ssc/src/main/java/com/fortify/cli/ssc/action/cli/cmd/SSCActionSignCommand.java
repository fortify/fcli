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

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.crypto.SignatureHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "sign")
public class SSCActionSignCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin OutputHelperMixins.TableNoQuery outputHelper;
    @Option(names = "--in", required=true) private Path actionFileToSign;
    @Option(names = "--out", required=true) private Path signedActionFile;
    @Option(names = "--info", required=false) private Path infoFile;
    @Option(names="--with", required=true) private Path privateKeyPath;
    @Option(names="--pubout", required=false) private Path publicKeyPath;
    @Option(names = {"--password", "-p"}, interactive = true, echo = false, arity = "0..1", required = false) 
    private char[] privateKeyPassword;
    
    @Override @SneakyThrows
    public JsonNode getJsonNode() {
        if ( !Files.exists(privateKeyPath) ) {
            if ( publicKeyPath==null ) {
                throw new IllegalStateException("Private key file "+privateKeyPath+" doesn't exist, and not generating new key file as --pubOut hasn't been specified");
            } else {
                SignatureHelper.keyPairGenerator(privateKeyPassword).writePem(privateKeyPath, publicKeyPath);
            }
        }
        
        
        var signer = SignatureHelper.textSigner(privateKeyPath, privateKeyPassword);
        signer.signAndWrite(actionFileToSign, signedActionFile, null);
        
        var fortifyFingerPrint = SignatureHelper.fortifySignatureVerifier().publicKeyFingerPrint();
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("fingerprint", signer.publicKeyFingerprint())
                .put("fortifyFingerprint", fortifyFingerPrint);
        
    }

    
    
    @Override
    public String getActionCommandResult() {
        return "GENERATED";
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
