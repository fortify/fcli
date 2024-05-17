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
package com.fortify.cli.common.crypto.helper.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PublicKey;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureMetadata;

import lombok.SneakyThrows;

public final class TextSigner {
    private final Signer signer;
    
    public TextSigner(byte[] privateKey, char[] passPhrase) {
        this.signer = new Signer(privateKey, passPhrase);
    }
    
    public TextSigner(String pemOrBase64Key, char[] passPhrase) {
        this(InternalSignatureUtil.parseKey(pemOrBase64Key), passPhrase);
    }
    
    @SneakyThrows
    public final void signAndWrite(Path payloadPath, Path outputPath, SignatureMetadata metadata) {
        var content = sign(Files.readString(payloadPath), metadata);
        Files.writeString(outputPath, content, StandardOpenOption.CREATE_NEW);
    }
    
    @SneakyThrows
    public final String sign(Path payloadPath, SignatureMetadata metadata) {
        return sign(Files.readString(payloadPath), metadata);
    }
    
    @SneakyThrows
    public final String sign(String textToSign, SignatureMetadata metadata) {
        var payload = readBytes(textToSign);
        var fingerprint = signer.publicKeyFingerprint();
        var signature = signer.sign(payload);
        
        var signatureDescriptor = SignatureDescriptor.builder()
                .signature(signature)
                .publicKeyFingerprint(fingerprint)
                .metadata(metadata)
                .build();
        return generateOutput(payload, signatureDescriptor);
    }

    private byte[] readBytes(String payload) throws IOException {
        if ( payload.contains(String.valueOf(InternalSignatureUtil.FILE_SEPARATOR)) ) {
            throw new IllegalStateException("Input file may not contain Unicode File Separator character \u001C");
        }
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    @SneakyThrows
    private final String generateOutput(byte[] payload, SignatureDescriptor signatureDescriptor) {
        YAMLFactory factory = new YAMLFactory();
        try ( var os = new ByteArrayOutputStream();
              var generator = (YAMLGenerator)factory.createGenerator(os) ) {
            os.write(payload);
            os.write(InternalSignatureUtil.FILE_SEPARATOR);
            os.write('\n');
            generator.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                    .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, true)
                    .setCodec(new ObjectMapper())
                    .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                    .useDefaultPrettyPrinter();
            generator.writeObject(signatureDescriptor);
            return os.toString(StandardCharsets.UTF_8);
        }
    }

    public final PublicKey publicKey() {
        return signer.publicKey();
    }
    
    public final String publicKeyFingerprint() {
        return signer.publicKeyFingerprint();
    }
    
    public final void writePublicKey(Path publicKeyPath) {
        InternalSignatureUtil.writePem("PUBLIC KEY", publicKey().getEncoded(), publicKeyPath);
    }
}