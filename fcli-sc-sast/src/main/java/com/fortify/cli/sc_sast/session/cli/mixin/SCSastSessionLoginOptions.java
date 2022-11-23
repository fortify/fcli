package com.fortify.cli.sc_sast.session.cli.mixin;

import com.fortify.cli.ssc.session.manager.ISSCCredentialsConfig;
import com.fortify.cli.ssc.session.manager.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.token.helper.SSCTokenConverter;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCSastSessionLoginOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SCSastUrlConfigOptions urlConfigOptions = new SCSastUrlConfigOptions();
    
    @Option(names = {"--client-auth-token", "-c"}, required = true, interactive = true, arity = "0..1", echo = false) 
    @Getter private char[] clientAuthToken;
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 2)
    @Getter private SSCAuthOptions authOptions = new SSCAuthOptions();
    
    public ISSCCredentialsConfig getCredentialsConfig() {
        return authOptions==null ? null : authOptions.getCredentialOptions();
    }
    
    public ISSCUserCredentialsConfig getUserCredentialsConfig() {
        return getCredentialsConfig()==null ? null : getCredentialsConfig().getUserCredentialsConfig();
    }
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 3)
        @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
    }
    
    public static class SSCCredentialOptions implements ISSCCredentialsConfig {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SCSastUserCredentialOptions userCredentialsConfig = new SCSastUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SCDastTokenCredentialOptions tokenOptions = new SCDastTokenCredentialOptions();
        
        @Override
        public char[] getPredefinedToken() {
            return tokenOptions==null || tokenOptions.token==null ? null : SSCTokenConverter.toRestToken(tokenOptions.token);
        }
    }
    
    public static class SCDastTokenCredentialOptions {
        @Option(names = {"--ssc-ci-token", "-t"}, required = true, interactive = true, echo = false)
        @Getter private char[] token;
    }
}
