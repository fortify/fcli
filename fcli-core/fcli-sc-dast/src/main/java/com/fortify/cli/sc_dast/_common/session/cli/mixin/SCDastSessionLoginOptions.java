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
package com.fortify.cli.sc_dast._common.session.cli.mixin;

import com.fortify.cli.ssc._common.session.helper.ISSCCredentialsConfig;
import com.fortify.cli.ssc.access_control.helper.SSCTokenConverter;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCDastSessionLoginOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SCDastUrlConfigOptions urlConfigOptions = new SCDastUrlConfigOptions();
    
    @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
    @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
    
    public static class SSCCredentialOptions implements ISSCCredentialsConfig {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SCDastUserCredentialAndExpiryOptions userCredentialsConfig = new SCDastUserCredentialAndExpiryOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SCDastTokenCredentialOptions tokenOptions = new SCDastTokenCredentialOptions();
        
        @Override
        public char[] getPredefinedToken() {
            return tokenOptions==null || tokenOptions.getToken()==null ? null : SSCTokenConverter.toRestToken(tokenOptions.getToken());
        }
    }
    
    public static class SCDastTokenCredentialOptions {
        @Option(names = {"--ssc-ci-token", "-t"}, interactive = true, echo = false, arity = "0..1", required = true)
        @Getter private char[] token;
    }
}
