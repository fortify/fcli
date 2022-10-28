package com.fortify.cli.sc_dast.session.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCDastSessionLogoutOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SSCAuthOptions authOptions = new SSCAuthOptions();
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
        @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
    }
    
    public static class SSCCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SCDastUserCredentialOptions userOptions= new SCDastUserCredentialOptions();
        @Option(names={"--no-revoke-token"})
        @Getter private boolean noRevokeToken;
    }
    
    public SCDastUserCredentialOptions getUserCredentialOptions() {
        return authOptions.credentialOptions.noRevokeToken ? null : authOptions.credentialOptions.userOptions;
    }
}
