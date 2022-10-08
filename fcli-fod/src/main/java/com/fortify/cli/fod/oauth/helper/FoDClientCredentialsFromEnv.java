package com.fortify.cli.fod.oauth.helper;

import static com.fortify.cli.common.util.EnvHelper.*;

import com.fortify.cli.fod.util.FoDConstants;

import lombok.Data;

@Data
public final class FoDClientCredentialsFromEnv implements IFoDClientCredentials {
    private final static String clientIdEnvName = envName(FoDConstants.PRODUCT_ENV_ID, "CLIENT_ID");
    private final static String clientSecretEnvName = envName(FoDConstants.PRODUCT_ENV_ID, "CLIENT_SECRET");
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