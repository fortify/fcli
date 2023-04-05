package com.fortify.cli.sc_dast.session.cli.mixin;

import com.fortify.cli.ssc.entity.token.helper.SSCTokenConverter;
import com.fortify.cli.ssc.session.helper.ISSCCredentialsConfig;

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
            return tokenOptions==null || tokenOptions.token==null ? null : SSCTokenConverter.toRestToken(tokenOptions.token);
        }
    }
    
    public static class SCDastTokenCredentialOptions {
        @Option(names = {"--ssc-ci-token", "-t"}, interactive = true, echo = false, arity = "0..1", required = true)
        @Getter private char[] token;
    }
}
