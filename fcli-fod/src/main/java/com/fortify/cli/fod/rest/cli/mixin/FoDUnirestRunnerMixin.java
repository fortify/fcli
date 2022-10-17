package com.fortify.cli.fod.rest.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.AbstractUnirestRunnerMixin;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.fod.oauth.helper.FoDOAuthHelper;
import com.fortify.cli.fod.session.manager.FoDSessionData;
import com.fortify.cli.fod.session.manager.FoDSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@ReflectiveAccess @FixInjection
public class FoDUnirestRunnerMixin extends AbstractUnirestRunnerMixin<FoDSessionData, FoDSessionDataManager> {
    @Inject @Getter private FoDSessionDataManager sessionDataManager;
    @Inject private FoDOAuthHelper oauthHelper;
    
    /**
     * Return an FoDSessionData instance. If no session name has been explicitly specified,
     * and session data is available from the environment, the {@link FoDSessionData} instance
     * will be generated based on environment variables, otherwise {@link FoDSessionDataManager}
     * will be used to retrieve a previously persisted session.
     * @return
     */
    @Override
    protected final FoDSessionData getSessionData() {
        return sessionDataManager.get(getSessionNameMixin().getSessionName(), true);
    }

    @Override
    protected final void configure(UnirestInstance unirest, FoDSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getUrlConfig());
        final String authHeader = String.format("Bearer %s", sessionData.getActiveBearerToken());
        unirest.config().addDefaultHeader("Authorization", authHeader);
    }
    
    @Override
    protected final void cleanup(UnirestInstance unirest, FoDSessionData sessionData) {
        // TODO Once we implement automated login based on env vars, we can clean up any generated tokens here if necessary
    }
}
