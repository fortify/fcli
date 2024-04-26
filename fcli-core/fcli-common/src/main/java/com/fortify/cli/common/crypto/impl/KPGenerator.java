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

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public final class KPGenerator {
    private final char[] passPhrase;
    
    @SneakyThrows
    public final KeyPair generate() {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048); 
        return kpg.generateKeyPair();
    }
    
    @SneakyThrows
    public final void writePem(Path privateKeyPath, Path publicKeyPath) {
        var kp = generate();
        InternalSignatureUtil.writePem("PRIVATE KEY", kp.getPrivate().getEncoded(), passPhrase, privateKeyPath);
        InternalSignatureUtil.writePem("PUBLIC KEY", kp.getPublic().getEncoded(), publicKeyPath);
    }
}