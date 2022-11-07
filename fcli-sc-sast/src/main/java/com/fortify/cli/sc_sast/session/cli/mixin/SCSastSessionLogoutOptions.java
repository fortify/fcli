package com.fortify.cli.sc_sast.session.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCSastSessionLogoutOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SSCAuthOptions authOptions = new SSCAuthOptions();
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
        @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
    }
    
    public static class SSCCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SCSastUserCredentialOptions userOptions = new SCSastUserCredentialOptions();
        @Option(names={"--no-revoke-token"})
        @Getter private boolean noRevokeToken;
    }
    
    public SCSastUserCredentialOptions getUserCredentialOptions() {
        return authOptions.credentialOptions.noRevokeToken ? null : authOptions.credentialOptions.userOptions;
    }
}
