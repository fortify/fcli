package com.fortify.cli.ssc.session.manager;

import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public final class SSCSessionDataFromEnv extends SSCSessionData {
    private static final SSCUrlConfigFromEnv urlConfig = new SSCUrlConfigFromEnv();
    private static final SSCCredentialsConfigFromEnv credentialsConfig = new SSCCredentialsConfigFromEnv();
    
    private final SSCTokenHelper tokenHelper;
    
    public SSCSessionDataFromEnv(SSCTokenHelper tokenHelper) {
        super(urlConfig, credentialsConfig, tokenHelper);
        this.tokenHelper = tokenHelper;
    }
    
    public boolean hasConfigFromEnv() {
        return urlConfig.hasConfigFromEnv();
    }
    
    public void cleanup() {
        if (hasActiveCachedTokenResponse()) {
            logout(tokenHelper, credentialsConfig.getUserCredentialsConfig());
        }
    }
}