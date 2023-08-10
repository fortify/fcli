/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.token.helper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;


public final class SSCTokenConverter {
    private static Pattern applicationTokenPattern = Pattern.compile("^[\\da-f]{8}(?:-[\\da-f]{4}){3}-[\\da-f]{12}$");
    private SSCTokenConverter() {}
    
    public static final String toApplicationToken(String token) {
        return validateTokenFormat(isApplicationToken(token) ? token : decode(token)); 
    }
    
    public static final char[] toApplicationToken(char[] token) {
        return toApplicationToken(new String(token)).toCharArray();
    }
    
    public static final String toRestToken(String token) {
        return validateTokenFormat(isApplicationToken(token) ? encode(token) : token); 
    }
    
    public static final char[] toRestToken(char[] token) {
        return toRestToken(new String(token)).toCharArray();
    }
    
    public static final boolean isApplicationToken(String token) {
        return applicationTokenPattern.matcher(token).matches();
    }
    
    private static final String decode(String token) {
        return new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
    }
    
    private static final String encode(String token) {
        return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }
    
    private static final String validateTokenFormat(String token) {
        if ( !isApplicationToken(token) && !isApplicationToken(decode(token)) ) {
            throw new IllegalArgumentException("The provided token could not be decoded to a valid application token format");
        }
        return token;
    }
}
