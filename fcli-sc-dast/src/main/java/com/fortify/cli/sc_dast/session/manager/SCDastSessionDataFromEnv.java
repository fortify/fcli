package com.fortify.cli.sc_dast.session.manager;

import com.fortify.cli.ssc.session.manager.SSCCredentialsConfigFromEnv;
import com.fortify.cli.ssc.session.manager.SSCUrlConfigFromEnv;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public final class SCDastSessionDataFromEnv extends SCDastSessionData {
    private static final SSCUrlConfigFromEnv urlConfig = new SSCUrlConfigFromEnv();
    private static final SSCCredentialsConfigFromEnv credentialsConfig = new SSCCredentialsConfigFromEnv();
    
    private final SSCTokenHelper tokenHelper;
    
    public SCDastSessionDataFromEnv(SSCTokenHelper tokenHelper) {
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