/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
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
package com.fortify.cli.ssc.entity.token.helper;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;

public final class SSCTokenConverter {
    private SSCTokenConverter() {}
    
    public static final String toApplicationToken(String token) {
        return isApplicationToken(token) ? token : decode(token); 
    }
    
    public static final char[] toApplicationToken(char[] token) {
        return toApplicationToken(new String(token)).toCharArray();
    }
    
    public static final String toRestToken(String token) {
        return isApplicationToken(token) ? encode(token) : token; 
    }
    
    public static final char[] toRestToken(char[] token) {
        return toRestToken(new String(token)).toCharArray();
    }
    
    public static final boolean isApplicationToken(String token) {
        return token.matches("^[\\da-f]{8}(?:-[\\da-f]{4}){3}-[\\da-f]{12}$");
    }
    
    private static final String decode(String token) {
        return new String(Base64.decodeBase64(token), StandardCharsets.UTF_8);
    }
    
    private static final String encode(String token) {
        return Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8));
    }
}
