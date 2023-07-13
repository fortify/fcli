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
package com.fortify.cli.sc_sast._common.session.cli.mixin;

import com.fortify.cli.ssc._common.session.helper.ISSCCredentialsConfig;
import com.fortify.cli.ssc.token.helper.SSCTokenConverter;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCSastSessionLoginOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SCSastUrlConfigOptions urlConfigOptions = new SCSastUrlConfigOptions();
    
    @Option(names = {"--client-auth-token", "-c"}, required = true, interactive = true, arity = "0..1", echo = false) 
    @Getter private char[] clientAuthToken;
    
    @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
    @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 3)
        @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
    }
    
    public static class SSCCredentialOptions implements ISSCCredentialsConfig {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SCSastUserCredentialAndExpiryOptions userCredentialsConfig = new SCSastUserCredentialAndExpiryOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SCDastTokenCredentialOptions tokenOptions = new SCDastTokenCredentialOptions();
        
        @Override
        public char[] getPredefinedToken() {
            return tokenOptions==null || tokenOptions.token==null ? null : SSCTokenConverter.toRestToken(tokenOptions.token);
        }
    }
    
    public static class SCDastTokenCredentialOptions {
        // Note that the SCSastControllerScanStartCommand requires this predefined token to be
        // a CIToken. If we ever add support for passing arbitrary tokens (i.e. through a new 
        // --ssc-token option), we should be sure that we can distinguish between token passed
        // through --ssc-ci-token or --ssc-token.
        @Option(names = {"--ssc-ci-token", "-t"}, interactive = true, echo = false, arity = "0..1", required = true)
        @Getter private char[] token;
    }
}
