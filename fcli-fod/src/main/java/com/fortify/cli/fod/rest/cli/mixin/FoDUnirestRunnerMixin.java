package com.fortify.cli.fod.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.rest.runner.config.UrlConfigFromEnv;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.fod.oauth.helper.FoDClientCredentialsFromEnv;
import com.fortify.cli.fod.oauth.helper.FoDOAuthHelper;
import com.fortify.cli.fod.oauth.helper.FoDUserCredentialsFromEnv;
import com.fortify.cli.fod.session.manager.FoDSessionData;
import com.fortify.cli.fod.session.manager.FoDSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public class FoDUnirestRunnerMixin {
    @Inject private UnirestRunner runner;
    @Inject private FoDSessionDataManager sessionDataManager;
    @Inject private FoDOAuthHelper oauthHelper;
    @Mixin private SessionNameMixin sessionNameMixin;
    
    public <R> R run(Function<UnirestInstance, R> f) {
        return runner.run(unirest->run(unirest, f));
    }
    
    private <R> R run(UnirestInstance unirest, Function<UnirestInstance, R> f) {
        FoDSessionData sessionData = getSessionData();
        configure(unirest, sessionData);
        try {
            return f.apply(unirest);
        } finally {
            cleanup(unirest, sessionData);
        }
    }
    
    /**
     * Return an FoDSessionData instance. If no session name has been explicitly specified,
     * and session data is available from the environment, the {@link FoDSessionData} instance
     * will be generated based on environment variables, otherwise {@link FoDSessionDataManager}
     * will be used to retrieve a previously persisted session.
     * @return
     */
    private FoDSessionData getSessionData() {
        UrlConfigFromEnv urlConfig = new UrlConfigFromEnv("FCLI_FOD"); // TODO Use constant shared with FoD*FromEnv
        FoDClientCredentialsFromEnv clientCreds = new FoDClientCredentialsFromEnv();
        FoDUserCredentialsFromEnv userCreds = new FoDUserCredentialsFromEnv();
        boolean useEnv = !sessionNameMixin.hasSessionName() && urlConfig.hasConfigFromEnv();
        if ( !useEnv ) {
            return sessionDataManager.get(sessionNameMixin.getSessionName(), true);
        } else {
            String scopesFromEnv = EnvHelper.env("FCLI_FOS_SCOPES");
            String[] scopes = scopesFromEnv==null ? new String[]{"api-tenant"} : scopesFromEnv.split(","); 
            if ( clientCreds.hasConfigFromEnv() ) {
                return new FoDSessionData(urlConfig, oauthHelper.createToken(urlConfig, clientCreds, scopes));
            } else if ( userCreds.hasConfigFromEnv() ) {
                return new FoDSessionData(urlConfig, oauthHelper.createToken(urlConfig, userCreds, scopes));
            } else {
                throw new IllegalStateException("If "+urlConfig.getUrlEnvName()+" environment variable has been configured, user or client credentials need to be provided as environment variables as well");
            }
        }
    }

    private void configure(UnirestInstance unirest, FoDSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getUrlConfig());
        final String authHeader = String.format("Bearer %s", sessionData.getActiveBearerToken());
        unirest.config().addDefaultHeader("Authorization", authHeader);
    }
    
    private void cleanup(UnirestInstance unirest, FoDSessionData sessionData) {
        // TODO Once we implement automated login based on env vars, we can clean up any generated tokens here if necessary
    }
    
    
}
