package com.fortify.cli.fod.rest.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.cli.mixin.AbstractSimpleUnirestRunnerMixin;
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

@FixInjection
public class FoDUnirestRunnerMixin extends AbstractSimpleUnirestRunnerMixin<FoDSessionData> {
    @Inject @ReflectiveAccess private FoDSessionDataManager sessionDataManager;
    @Inject @ReflectiveAccess private FoDOAuthHelper oauthHelper;
    
    @Override
    protected final FoDSessionData getSessionData(String sessionName) {
        return sessionDataManager.get(sessionName, true);
    }

    @Override
    protected final void configure(UnirestInstance unirest, FoDSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getUrlConfig());
        ProxyHelper.configureProxy(unirest, "fod", sessionData.getUrlConfig().getUrl());
        final String authHeader = String.format("Bearer %s", sessionData.getActiveBearerToken());
        unirest.config().addDefaultHeader("Authorization", authHeader);
    }
}
