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

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeySource;
import com.fortify.cli.common.util.FcliDataHelper;

public final class PublicKeyTrustStore {
    public static final PublicKeyTrustStore INSTANCE = new PublicKeyTrustStore();
    private PublicKeyTrustStore() {}
    
    public PublicKeyDescriptor importKey(String publicKey, String name) {
        var fingerprint = new Verifier(publicKey).publicKeyFingerPrint();
        if ( StringUtils.isBlank(name) ) { name=fingerprint; }
        var descriptor = new PublicKeyDescriptor(name, fingerprint, publicKey, PublicKeySource.EXTERNAL);
        FcliDataHelper.saveFile(publicKeyPath(fingerprint), descriptor, true);
        return descriptor;
    }
    
    public final PublicKeyDescriptor load(String nameOrFingerprint, boolean failIfNotFound) {
        var result = FcliDataHelper.readFile(publicKeyPath(nameOrFingerprint), PublicKeyDescriptor.class, false);
        if ( result==null ) {
            result = stream().filter(d->d.getName().equals(nameOrFingerprint))
                    .findFirst()
                    .orElseGet(()->{
                        if ( !failIfNotFound ) { return null; }
                        throw new IllegalArgumentException("No public key found with name or fingerprint "+nameOrFingerprint);
                    });
        }
        return result==null ? null : result.toBuilder().source(PublicKeySource.TRUSTSTORE).build();
    }
    
    public final PublicKeyDescriptor forFingerprint(String fingerprint, String... extraPublicKeys) {
        PublicKeyDescriptor result = load(fingerprint, false);
        // Try to locate public key for fingerprint from given extra public keys
        if ( result==null && extraPublicKeys!=null ) {
            for ( var extraPublicKey : extraPublicKeys ) {
                if ( StringUtils.isNotBlank(extraPublicKey) ) {
                    var verifier = new Verifier(extraPublicKey);
                    if ( fingerprint.equals(verifier.publicKeyFingerPrint()) ) {
                        result = PublicKeyDescriptor.builder()
                                .fingerprint(fingerprint)
                                .name(null)
                                .publicKey(extraPublicKey)
                                .source(PublicKeySource.EXTERNAL)
                                .build();
                    }
                }
            }
        }
        return result;
    }
    
    public final PublicKeyDescriptor delete(String nameOrFingerprint) {
        var descriptor = load(nameOrFingerprint, true);
        var publicKeyPath = publicKeyPath(descriptor.getFingerprint());
        FcliDataHelper.deleteFile(publicKeyPath, false);
        return descriptor;
    }
    
    public final Stream<PublicKeyDescriptor> stream() {
        return FcliDataHelper.listFilesInDir(publicKeysPath(), true)
                .map(path->load(path.toFile().getName(), false))
                .filter(Objects::nonNull);
    }
    
    private static final Path publicKeyPath(String fingerprint) {
        return publicKeysPath().resolve(fingerprint);
    }
    
    private static final Path publicKeysPath() {
        return FcliDataHelper.getFcliConfigPath().resolve("public-keys");
    }
}
