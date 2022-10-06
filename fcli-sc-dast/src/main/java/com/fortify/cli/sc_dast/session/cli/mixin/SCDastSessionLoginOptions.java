package com.fortify.cli.sc_dast.session.cli.mixin;

import com.fortify.cli.ssc.session.manager.ISSCCredentialsConfig;
import com.fortify.cli.ssc.session.manager.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.token.helper.SSCTokenConverter;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCDastSessionLoginOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SCDastUrlConfigOptions urlConfigOptions;
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 2)
    @Getter private SSCAuthOptions authOptions;
    
    public ISSCCredentialsConfig getCredentialsConfig() {
        return authOptions==null ? null : authOptions.getCredentialOptions();
    }
    
    public ISSCUserCredentialsConfig getUserCredentialsConfig() {
        return getCredentialsConfig()==null ? null : getCredentialsConfig().getUserCredentialsConfig();
    }
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 3)
        @Getter private SSCCredentialOptions credentialOptions;
    }
    
    public static class SSCCredentialOptions implements ISSCCredentialsConfig {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SCDastUserCredentialOptions userCredentialsConfig = new SCDastUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SCDastTokenCredentialOptions tokenOptions = new SCDastTokenCredentialOptions();
        
        @Override
        public char[] getPredefinedToken() {
            return tokenOptions==null || tokenOptions.token==null ? null : SSCTokenConverter.toRestToken(tokenOptions.token);
        }
    }
    
    public static class SCDastTokenCredentialOptions {
        @Option(names = {"--ci-token", "-t"}, required = true, interactive = true, echo = false)
        @Getter private char[] token;
    }
}
