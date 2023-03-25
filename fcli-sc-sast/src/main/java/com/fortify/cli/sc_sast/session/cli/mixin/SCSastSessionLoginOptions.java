package com.fortify.cli.sc_sast.session.cli.mixin;

import com.fortify.cli.ssc.entity.token.helper.SSCTokenConverter;
import com.fortify.cli.ssc.session.helper.ISSCCredentialsConfig;

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
        @Getter private SCSastUserCredentialOptions userCredentialsConfig = new SCSastUserCredentialOptions();
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
