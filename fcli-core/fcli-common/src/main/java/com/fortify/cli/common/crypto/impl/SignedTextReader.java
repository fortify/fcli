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
package com.fortify.cli.common.crypto.impl;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.crypto.SignatureHelper.InvalidSignatureHandler;
import com.fortify.cli.common.crypto.SignatureHelper.SignatureDescriptor;
import com.fortify.cli.common.crypto.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.crypto.SignatureHelper.SignedTextDescriptor;
import com.fortify.cli.common.util.FileUtils;

import lombok.SneakyThrows;

public final class SignedTextReader {
    public static final SignedTextReader INSTANCE = new SignedTextReader();
    private SignedTextReader() {}
    public final SignedTextDescriptor load(InputStream is, Charset charset, boolean evaluateSignature) {
        return load(FileUtils.readInputStreamAsString(is, charset), evaluateSignature);
    }
    
    public final SignedTextDescriptor load(InputStream is, Charset charset, InvalidSignatureHandler onInvalidSingature) {
        return load(FileUtils.readInputStreamAsString(is, charset), onInvalidSingature);
    }
    
    @SneakyThrows
    public final SignedTextDescriptor load(String signedOrUnsignedText, boolean evaluateSignature) {
        var elts = signedOrUnsignedText.split(String.valueOf(InternalSignatureUtil.FILE_SEPARATOR));
        if ( elts.length>2 ) {
            throw new IllegalStateException("Input may contain only single Unicode File Separator character");
        } else if ( elts.length==1) {
            return buildUnsignedDescriptor(elts[0]);
        } else {
            var signatureDescriptor = new ObjectMapper(new YAMLFactory())
                    .readValue(elts[1], SignatureDescriptor.class);
            return buildSignedDescriptor(signedOrUnsignedText, elts[0], signatureDescriptor, evaluateSignature);
        }
    }
    
    /**
     * Load a {@link SignedTextDescriptor} instance from the given signed or
     * unsigned text.
     * 
     * @param signedOrUnsignedText Either signed or unsigned text to be loaded.
     * @param onInvalidSingature is invoked if there's no valid signature. If null,
     *        the signature status will not be evaluated. To evaluate signature status
     *        without performing any action on failure, simply pass d->{}.
     * @return {@link SignedTextDescriptor} instance
     */
    public final SignedTextDescriptor load(String signedOrUnsignedText, InvalidSignatureHandler onInvalidSingature) {
        if ( onInvalidSingature==null ) { return load(signedOrUnsignedText, false); }
        var descriptor = load(signedOrUnsignedText, true);
        if ( descriptor.getSignatureStatus()!=SignatureStatus.VALID_SIGNATURE ) {
            onInvalidSingature.onInvalidSignature(descriptor);
        }
        return descriptor;
    }

    private SignedTextDescriptor buildUnsignedDescriptor(String text) {
        return SignedTextDescriptor.builder()
                .rawText(text)
                .text(text)
                .signatureStatus(SignatureStatus.NO_SIGNATURE)
                .build();
    }
    
    private SignedTextDescriptor buildSignedDescriptor(String rawText, String text, SignatureDescriptor signatureDescriptor, boolean evaluateSignatureStatus) {
        var signatureStatus = SignatureStatus.NOT_VERIFIED;
        if ( evaluateSignatureStatus ) {
            var fingerprint = signatureDescriptor.getPublicKeyFingerprint();
            var expectedSignature = signatureDescriptor.getSignature();
            signatureStatus = Verifier.forFingerprint(fingerprint)
                .verify(text, StandardCharsets.UTF_8, expectedSignature);
        }
        return SignedTextDescriptor.builder()
                .rawText(rawText)
                .text(text)
                .signatureDescriptor(signatureDescriptor)
                .signatureStatus(signatureStatus)
                .build();
    }

}