package com.fortify.cli.fod.oauth.helper;

import static com.fortify.cli.common.util.EnvHelper.*;

import lombok.Data;

@Data
public final class FoDClientCredentialsFromEnv implements IFoDClientCredentials {
    private static final String PFX = "FCLI_FOD";
    private final static String clientIdEnvName = envName(PFX, "CLIENT_ID");
    private final static String clientSecretEnvName = envName(PFX, "CLIENT_SECRET");
    private final String clientId;
    private final String clientSecret;
    
    public FoDClientCredentialsFromEnv() {
        this.clientId = env(clientIdEnvName);
        this.clientSecret = env(clientSecretEnvName);
        checkBothOrNone(clientIdEnvName, clientSecretEnvName);
    }
    
    public boolean hasConfigFromEnv() {
        return clientId!=null;
    }
}