package com.fortify.cli.ssc.session.manager;

import static com.fortify.cli.common.util.EnvHelper.*;

import com.fortify.cli.ssc.util.SSCConstants;

import lombok.Data;

@Data
public final class SSCCredentialsConfigFromEnv implements ISSCCredentialsConfig {
    private final static String tokenEnvName = envName(SSCConstants.PRODUCT_ENV_ID, "TOKEN");
    private final char[] predefinedToken;
    private final SSCUserCredentialsConfigFromEnv userCredentialsConfig;
    
    public SSCCredentialsConfigFromEnv() {
        this.predefinedToken = asCharArray(env(tokenEnvName));
        this.userCredentialsConfig = new SSCUserCredentialsConfigFromEnv();
    }
    
    public boolean hasConfigFromEnv() {
        return predefinedToken!=null || userCredentialsConfig.hasConfigFromEnv();
    }
}