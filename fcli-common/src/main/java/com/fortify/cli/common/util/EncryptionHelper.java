/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

import lombok.RequiredArgsConstructor;

public class EncryptionHelper {
    private static final StandardPBEStringEncryptor encryptor = createAES256TextEncryptor();
    public static final String encrypt(String source) {
        if ( source==null ) { return null; }
        return encryptor.encrypt(source);
    }

    public static final String decrypt(String source) {
        if ( source==null ) { return null; }
        return encryptor.decrypt(source);
    }
    
    private static final StandardPBEStringEncryptor createAES256TextEncryptor() {
        var encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.setPassword(getEncryptPassword());
        return encryptor;
    }
    
    private static final String getEncryptPassword() {
        String userPassword = System.getenv("FCLI_ENCRYPT_KEY");
        userPassword = StringUtils.isBlank(userPassword) ? "" : userPassword;
        return userPassword+"ds$%YTjdwaf#$47672dfdsGVFDa";
    }
    
    @RequiredArgsConstructor
    // TODO Can we optimize this to not buffer the full contents before encrypting and writing the output?
    public static final class EncryptWriter extends StringWriter {
        private final Writer originalWriter;
        
        @Override
        public void close() throws IOException {
            originalWriter.write(encrypt(getBuffer().toString()));
            originalWriter.flush();
            originalWriter.close();
        }
    }
}
