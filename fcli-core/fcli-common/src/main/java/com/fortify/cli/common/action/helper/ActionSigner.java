/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.helper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PublicKey;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fortify.cli.common.action.model.SignedAction;
import com.fortify.cli.common.action.model._ActionRoot;
import com.fortify.cli.common.crypto.SignatureHelper;
import com.fortify.cli.common.crypto.SignatureHelper.Signer;

import lombok.SneakyThrows;

public final class ActionSigner {
    private final Signer signer;
    
    public ActionSigner(Path privateKeyPath, char[] passPhrase) {
        this.signer = SignatureHelper.signer(privateKeyPath, passPhrase);
    }
    
    @SneakyThrows
    public final void sign(Path actionFileToSign, Path signedActionFile) {
        var action = Files.readAllBytes(actionFileToSign);
        var fingerprint = signer.publicKeyFingerprint();
        var signature = signer.sign(action, true);
        
        var actionRoot = new _ActionRoot();
        var signedAction = new SignedAction();
        signedAction.setSignature(signature);
        signedAction.setPublicKeyFingerprint(fingerprint);
        signedAction.setActionBase64(Base64.getEncoder().encodeToString(action));
        // TODO Add info
        actionRoot.setSignedAction(signedAction);
        write(signedActionFile, actionRoot);
        //Files.writeString(signedActionFile, actionRoot.toString(), StandardCharsets.UTF_8);
    }
    
    @SneakyThrows
    private void write(Path signedActionFile, _ActionRoot actionRoot) {
        YAMLFactory factory = new YAMLFactory();
        try ( var writer = Files.newBufferedWriter(signedActionFile, StandardOpenOption.CREATE_NEW);
              var generator = (YAMLGenerator)factory.createGenerator(writer) ) {
            generator.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                    .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                    .setCodec(new ObjectMapper())
                    .disable(Feature.AUTO_CLOSE_TARGET)
                    .useDefaultPrettyPrinter();
            generator.writeObject(actionRoot);
        }
    }

    public final PublicKey publicKey() {
        return signer.publicKey();
    }
    
    public final String publicKeyFingerprint() {
        return signer.publicKeyFingerprint();
    }
    
    public final void writePublicKey(Path publicKeyPath) {
        // TODO
    }
}
