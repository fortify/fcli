package com.fortify.cli.sc_sast.session.cli.mixin;

import java.util.Optional;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCSastSessionLogoutOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SSCAuthOptions authOptions;
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
        @Getter private SSCCredentialOptions credentialOptions;
    }
    
    public static class SSCCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SCSastUserCredentialOptions userOptions;
        @Option(names={"--no-revoke-token"})
        @Getter private boolean noRevokeToken; // If this option is specified, then userOptions will be null and token will not be revoked
    }
    
    public SCSastUserCredentialOptions getUserCredentialOptions() {
        return Optional.ofNullable(authOptions).map(SSCAuthOptions::getCredentialOptions).map(SSCCredentialOptions::getUserOptions).orElse(null);
    }
}
