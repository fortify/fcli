/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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
