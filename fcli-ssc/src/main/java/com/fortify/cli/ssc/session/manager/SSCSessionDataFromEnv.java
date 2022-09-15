package com.fortify.cli.ssc.session.manager;

import static com.fortify.cli.common.util.EnvHelper.*;

import com.fortify.cli.common.rest.runner.config.IUserCredentials;
import com.fortify.cli.common.rest.runner.config.UrlConfigFromEnv;
import com.fortify.cli.ssc.token.helper.SSCTokenCreateRequest;
import com.fortify.cli.ssc.token.helper.SSCTokenCreateResponse;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public final class SSCSessionDataFromEnv extends SSCSessionData implements IUserCredentials {
    private static final String PFX = "FCLI_SSC";
    private final static String tokenEnvName = envName(PFX, "TOKEN");
    private final static String userEnvName = envName(PFX, "USER");
    private final static String passwordEnvName = envName(PFX, "PASSWORD");
    private final SSCTokenHelper tokenHelper;
    private final String user;
    private final char[] password;
    
    public SSCSessionDataFromEnv(SSCTokenHelper tokenHelper) {
        super(new UrlConfigFromEnv(PFX));
        this.tokenHelper = tokenHelper;
        setPredefinedToken(asCharArray(env(tokenEnvName)));
        this.user = env(userEnvName);
        this.password = asCharArray(env(passwordEnvName));
        checkEnv();
        if ( this.user!=null ) {
            SSCTokenCreateRequest tokenCreateRequest = SSCTokenCreateRequest.builder()
                .description("Auto-generated for fcli transient session")
                .type("UnifiedLoginToken").build();
            setCachedTokenResponse(tokenHelper.createToken(getUrlConfig(), this, tokenCreateRequest, SSCTokenCreateResponse.class));
        }
    }
    
    public boolean hasConfigFromEnv() {
        return getUrlConfig().hasConfigFromEnv();
    }
    
    @Override
    public UrlConfigFromEnv getUrlConfig() {
        return (UrlConfigFromEnv)super.getUrlConfig();
    }
    
    public void cleanup() {
        if (hasActiveCachedTokenResponse()) {
            tokenHelper.deleteTokensById(getUrlConfig(), this, getTokenId());
        }
    }
    
    private void checkEnv() {
        String urlEnvName = getUrlConfig().getUrlEnvName();
        checkSecondaryWithoutPrimary(tokenEnvName, urlEnvName);
        checkSecondaryWithoutPrimary(userEnvName, urlEnvName);
        checkExclusive(tokenEnvName, userEnvName);
        checkBothOrNone(passwordEnvName, userEnvName);
        if ( hasConfigFromEnv() ) {
            if ( getPredefinedToken()==null && this.user==null ) {
                throw new IllegalStateException("Either "+tokenEnvName+" or "+userEnvName+" environment variable must be configured if "+urlEnvName+" has been configured");
            }
        }
    }
}