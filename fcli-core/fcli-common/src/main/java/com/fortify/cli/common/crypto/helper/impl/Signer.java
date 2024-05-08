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
import java.nio.file.Path;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.fortify.cli.common.crypto.helper.impl.InternalSignatureUtil.DataSignatureUpdater;
import com.fortify.cli.common.crypto.helper.impl.InternalSignatureUtil.FileSignatureUpdater;
import com.fortify.cli.common.crypto.helper.impl.InternalSignatureUtil.ISignatureUpdater;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public final class Signer {
    private final byte[] privateKey;
    private final char[] passPhrase;
    
    public Signer(String pemOrBase64Key, char[] passPhrase) {
        this(InternalSignatureUtil.parseKey(pemOrBase64Key), passPhrase);
    }
    
    @SneakyThrows
    public final PublicKey publicKey() {
        var privateKey = (RSAPrivateCrtKey)createKey();
        var keySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
        return InternalSignatureUtil.KEY_FACTORY.generatePublic(keySpec);
    }
    
    @SneakyThrows
    public final String publicKeyFingerprint() {
        var encodedKey = publicKey().getEncoded();
        byte[] hash = MessageDigest.getInstance("SHA256").digest(encodedKey);
        String hexKey = String.format("%064X", new BigInteger(1, hash));
        return hexKey;
    }
    
    public final byte[] signAsBytes(File file) {
        return signAsBytes(new FileSignatureUpdater(file));
    }
    
    public final byte[] signAsBytes(byte[] data) {
        return signAsBytes(new DataSignatureUpdater(data));
    }
    
    public final byte[] signAsBytes(String data, Charset charset) {
        return signAsBytes(new DataSignatureUpdater(data, charset));
    }
    
    public final String sign(File file) {
        return sign(new FileSignatureUpdater(file));
    }
    
    public final String sign(Path path) {
        return sign(new FileSignatureUpdater(path.toFile()));
    }
    
    public final String sign(byte[] data) {
        return sign(new DataSignatureUpdater(data));
    }
    
    public final String sign(String data, Charset charset) {
        return sign(new DataSignatureUpdater(data, charset));
    }
    
    private final String sign(ISignatureUpdater updater) {
        var signature = signAsBytes(updater);
        return signature==null ? null : Base64.getEncoder().encodeToString(signature);
    }
    
    @SneakyThrows
    private final byte[] signAsBytes(ISignatureUpdater updater) {
        Signature signature = createSignature();
        updater.updateSignature(signature);
        return signature.sign();
    }
    
    @SneakyThrows
    private final Signature createSignature() {
        var key = createKey();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        return signature;
    }
    
    /** This works for AES-encrypted keys on Java 21 */
    @SneakyThrows
    private PrivateKey createKey() {
        var pkcs8KeySpec = passPhrase==null 
                ? new PKCS8EncodedKeySpec(privateKey)
                : decryptEncodedKeySpec(privateKey);
        return InternalSignatureUtil.KEY_FACTORY.generatePrivate(pkcs8KeySpec);
    }
     
    @SneakyThrows
    private PKCS8EncodedKeySpec decryptEncodedKeySpec(byte[] bytes) {
        EncryptedPrivateKeyInfo encryptPKInfo = new EncryptedPrivateKeyInfo(bytes);
        Cipher cipher = Cipher.getInstance(encryptPKInfo.getAlgName());
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passPhrase);
        SecretKeyFactory secFac = SecretKeyFactory.getInstance(encryptPKInfo.getAlgName());
        Key pbeKey = secFac.generateSecret(pbeKeySpec);
        AlgorithmParameters algParams = encryptPKInfo.getAlgParameters();
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
        return encryptPKInfo.getKeySpec(cipher);
    }
}