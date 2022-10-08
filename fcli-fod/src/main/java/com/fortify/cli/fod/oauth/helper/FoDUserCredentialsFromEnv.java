package com.fortify.cli.fod.oauth.helper;

import static com.fortify.cli.common.util.EnvHelper.*;

import com.fortify.cli.fod.util.FoDConstants;

import lombok.Data;

@Data
public final class FoDUserCredentialsFromEnv implements IFoDUserCredentials {
    private final static String tenantEnvName = envName(FoDConstants.PRODUCT_ENV_ID, "TENANT");
    private final static String userEnvName = envName(FoDConstants.PRODUCT_ENV_ID, "USER");
    private final static String passwordEnvName = envName(FoDConstants.PRODUCT_ENV_ID, "PASSWORD");
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