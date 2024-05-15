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
package com.fortify.cli.common.action.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.crypto.helper.impl.TextSigner;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class AbstractActionSignCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(AbstractActionSignCommand.class);
    @Getter @Mixin OutputHelperMixins.TableNoQuery outputHelper;
    @Option(names = "--in", required=true, descriptionKey="fcli.action.sign.in") 
    private Path actionFileToSign;
    @Option(names = "--out", required=true, descriptionKey="fcli.action.sign.out")
    private Path signedActionFile;
    @Option(names = "--info", required=false, descriptionKey="fcli.action.sign.info") 
    private Path extraInfoPath;
    @Option(names="--with", required=true, descriptionKey="fcli.action.sign.with") 
    private Path privateKeyPath;
    @Option(names="--pubout", required=false, descriptionKey="fcli.action.sign.pubout") 
    private Path publicKeyPath;
    @Option(names = {"--password", "-p"}, interactive = true, echo = false, arity = "0..1", required = false, descriptionKey="fcli.action.sign.password") 
    private char[] privateKeyPassword;
    @Mixin private CommonOptionMixins.RequireConfirmation confirm;
    
    @Override @SneakyThrows
    public JsonNode getJsonNode() {
        var keyPairCreated = createKeyPair();
        deleteExistingOutputFile();
        ObjectNode extraInfo = createExtraInfo();
        var signer = SignatureHelper.textSigner(privateKeyPath, privateKeyPassword);
        signer.signAndWrite(actionFileToSign, signedActionFile, extraInfo);
        writePublicKey(signer, !keyPairCreated);
        
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("in", actionFileToSign.toString())
                .put("out", signedActionFile.toString())
                .put("publicKeyFingerprint", signer.publicKeyFingerprint())
                .set("extraInfo", extraInfo);
    }

    private void writePublicKey(TextSigner signer, boolean doWritePublicKey) {
        if ( doWritePublicKey && publicKeyPath!=null ) {
            if ( Files.exists(publicKeyPath) ) {
                LOG.warn("WARN: Not writing public key as file already exists");
            } else {
                signer.writePublicKey(publicKeyPath);
            }
        }
        
    }



    private final ObjectNode createExtraInfo() {
        ObjectNode extraInfo = objectMapper.createObjectNode();
        if ( extraInfoPath!=null ) {
            try {
                extraInfo.setAll((ObjectNode)JsonHelper.getObjectMapper().valueToTree(Files.readString(extraInfoPath)));
            } catch ( Exception e ) {
                LOG.warn("WARN: Error parsing extra info file contents");
            }
        }
        extraInfo.put("fcli", FcliBuildPropertiesHelper.getFcliBuildInfo());
        return extraInfo;
    }

    private final void deleteExistingOutputFile() throws IOException {
        if ( Files.exists(signedActionFile) ) {
            confirm.checkConfirmed(signedActionFile.toString());
            Files.deleteIfExists(signedActionFile);
        }
    }

    private final boolean createKeyPair() {
        if ( Files.exists(privateKeyPath) ) {
            return false;
        } else {
            if ( publicKeyPath==null ) {
                throw new IllegalStateException("Private key file "+privateKeyPath+" doesn't exist, and not generating new key file as --pubOut hasn't been specified");
            } else {
                SignatureHelper.keyPairGenerator(privateKeyPassword).writePem(privateKeyPath, publicKeyPath);
                return true;
            }
        }
    }

    @Override
    public final String getActionCommandResult() {
        return "SIGNED";
    }
    
    @Override
    public final boolean isSingular() {
        return false;
    }
}
