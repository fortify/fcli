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
package com.fortify.cli.common.crypto.helper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.FileUtils;

import lombok.RequiredArgsConstructor;

/**
 * Instance-based helper for encrypting/decrypting text using a configured password.
 * A DEFAULT instance is provided for the standard fcli encryption password.
 */
public final class EncryptionHelper {
    public static final EncryptionHelper DEFAULT = new EncryptionHelper(getDefaultEncryptPassword());
    private final StandardPBEStringEncryptor encryptor;

    public EncryptionHelper(String password) {
        this.encryptor = createEncryptorWithPassword(password);
    }

    public void save(String contents, Path dest) {
        try {
            String encrypted = encrypt(contents);
            FileUtils.writeStringWithOwnerOnlyPermissions(dest, encrypted==null?"":encrypted);
        } catch ( IOException e ) {
            throw new FcliTechnicalException("Error writing encrypted file "+dest, e);
        }
    }

    public String read(Path src) {
        try {
            String raw = FileUtils.readString(src);
            return decrypt(raw);
        } catch ( IOException e ) {
            throw new FcliTechnicalException("Error reading encrypted file "+src, e);
        }
    }

    public String encrypt(String source) {
        if ( source==null ) { return null; }
        return encryptor.encrypt(source);
    }
    public String decrypt(String source) {
        if ( source==null ) { return null; }
        return encryptor.decrypt(source);
    }

    private static final StandardPBEStringEncryptor createEncryptorWithPassword(String password) {
        var enc = new StandardPBEStringEncryptor();
        enc.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        enc.setIvGenerator(new RandomIvGenerator());
        enc.setPassword(password==null?"":password);
        return enc;
    }

    private static final String getDefaultEncryptPassword() {
        String userPassword = EnvHelper.env("FCLI_ENCRYPT_KEY");
        userPassword = StringUtils.isBlank(userPassword) ? "" : userPassword;
        return userPassword+"ds$%YTjdwaf#$47672dfdsGVFDa";
    }

    @RequiredArgsConstructor
    // TODO Can we optimize this to not buffer the full contents before encrypting and writing the output?
    public final class EncryptWriter extends StringWriter {
        private final Writer originalWriter;

        @Override
        public void close() throws IOException {
            originalWriter.write(encrypt(getBuffer().toString()));
            originalWriter.flush();
            originalWriter.close();
        }
    }
}
