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

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.crypto.helper.impl.InternalSignatureUtil.DataSignatureUpdater;
import com.fortify.cli.common.crypto.helper.impl.InternalSignatureUtil.FileSignatureUpdater;
import com.fortify.cli.common.crypto.helper.impl.InternalSignatureUtil.ISignatureUpdater;
import com.fortify.cli.common.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public final class Verifier {
    private final byte[] publicKey;
    
    public Verifier(String pemOrBase64Key) {
        this(StringUtils.isBlank(pemOrBase64Key)
                ? null
                : InternalSignatureUtil.parseKey(pemOrBase64Key));
    }
    
    public Verifier(PublicKeyDescriptor publicKeyDescriptor) {
        this(publicKeyDescriptor==null 
                ? null
                : publicKeyDescriptor.getPublicKey());
    }
    
    public final SignatureStatus verify(File file, String expectedSignature) {
        return verify(new FileSignatureUpdater(file), expectedSignature);
    }
    
    public final SignatureStatus verify(byte[] data, String expectedSignature) {
        return verify(new DataSignatureUpdater(data), expectedSignature);
    }
    
    public final SignatureStatus verify(String data, Charset charset, String expectedSignature) {
        return verify(new DataSignatureUpdater(data, charset), expectedSignature);
    }
    
    @SneakyThrows
    private final SignatureStatus verify(ISignatureUpdater updater, String expectedSignature) {
        if ( publicKey()==null ) { return SignatureStatus.NO_PUBLIC_KEY; }
        Signature signature = createSignature();
        updater.updateSignature(signature);
        return verifySignature(signature, expectedSignature);
    }
    
    @SneakyThrows
    private final SignatureStatus verifySignature(Signature signature, String expectedSignature) {
        if(signature.verify(Base64.getDecoder().decode(expectedSignature))) {
            return SignatureStatus.VALID_SIGNATURE;
        } else {
            return SignatureStatus.INVALID_SIGNATURE;
        }
    }
    
    @SneakyThrows
    private final Signature createSignature() {
        PublicKey pub = publicKey();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(pub);
        return signature;
    }

    @SneakyThrows
    private PublicKey publicKey() {
        if ( publicKey==null ) { return null; }
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
        PublicKey pub = InternalSignatureUtil.KEY_FACTORY.generatePublic(spec);
        return pub;
    }

    @SneakyThrows
    public String publicKeyFingerPrint() {
        var publicKey = publicKey();
        if ( publicKey==null ) { return null; }
        var encodedKey = publicKey.getEncoded();
        byte[] hash = MessageDigest.getInstance("SHA256").digest(encodedKey);
        String hexKey = String.format("%064X", new BigInteger(1, hash));
        return hexKey;
    }
}