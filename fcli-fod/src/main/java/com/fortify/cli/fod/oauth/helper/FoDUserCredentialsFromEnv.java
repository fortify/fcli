package com.fortify.cli.fod.oauth.helper;

import static com.fortify.cli.common.util.EnvHelper.*;

import lombok.Data;

@Data
public final class FoDUserCredentialsFromEnv implements IFoDUserCredentials {
    private static final String PFX = "FCLI_FOD";
    private final static String tenantEnvName = envName(PFX, "TENANT");
    private final static String userEnvName = envName(PFX, "USER");
    private final static String passwordEnvName = envName(PFX, "PASSWORD");
    private final String tenant;
    private final String user;
    private final char[] password;
    
    public FoDUserCredentialsFromEnv() {
        this.tenant = env(tenantEnvName);
        this.user = env(userEnvName);
        this.password = asCharArray(env(passwordEnvName));
        checkBothOrNone(tenantEnvName, userEnvName);
        checkBothOrNone(userEnvName, passwordEnvName);
    }
    
    public boolean hasConfigFromEnv() {
        return user!=null;
    }
}