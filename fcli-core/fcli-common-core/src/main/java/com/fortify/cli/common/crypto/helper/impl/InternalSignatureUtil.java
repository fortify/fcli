/*
 * Copyright 2021-2026 Open Text.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

final class InternalSignatureUtil {
    // This character is used by TextFileSigner and SignedTextFileReader to 
    // separate original text document and signature YAML document.
    static final char FILE_SEPARATOR = '\u001C';
    static final KeyFactory KEY_FACTORY = InternalSignatureUtil.createKeyFactory();
    
    @SneakyThrows
    private static final KeyFactory createKeyFactory() {
        return KeyFactory.getInstance("RSA");
    }

    static final byte[] parseKey(String pemOrBase64Key) {
        if ( pemOrBase64Key==null ) { return null; }
        var base64 = pemOrBase64Key.replaceAll("-----(BEGIN|END) [\\sA-Z]+ KEY-----|\\s", "");
        return Base64.getDecoder().decode(base64);
    }
    
    @SneakyThrows
    static final void writePem(String type, byte[] key, Path path) {
        var pemString = asPem(type, key);
        Files.writeString(path, pemString, StandardOpenOption.CREATE_NEW);
    }
    
    private static final String asPem(String type, byte[] key) {
        return "-----BEGIN "+type+"-----\n"
                + Base64.getMimeEncoder().encodeToString(key)
                + "\n-----END "+type+"-----";
    }
    
    static interface ISignatureUpdater {
        void updateSignature(Signature signature) throws IOException, SignatureException;
    }
    
    @RequiredArgsConstructor
    static final class FileSignatureUpdater implements ISignatureUpdater {
        private final File file;
        
        @Override
        public void updateSignature(Signature signature) throws IOException, SignatureException {
            try ( var is = new FileInputStream(file); ) {
                byte[] buffer = new byte[4096];
                int read = 0;
                while ( (read = is.read(buffer)) > 0 ) {
                    signature.update(buffer, 0, read);
                }
            }
        }
    }
    
    @RequiredArgsConstructor
    static final class DataSignatureUpdater implements ISignatureUpdater {
        private final byte[] data;
        
        DataSignatureUpdater(String s, Charset charset) {
            this(s.getBytes(charset));
        }
        
        @Override
        public void updateSignature(Signature signature) throws IOException, SignatureException {
            signature.update(data);
        }
    }
}