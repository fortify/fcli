package com.fortify.cli.sc_dast.rest.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.cli.mixin.AbstractSimpleUnirestRunnerMixin;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_dast.session.manager.ISCDastSessionData;
import com.fortify.cli.sc_dast.session.manager.SCDastSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@FixInjection
public class SCDastUnirestRunnerMixin extends AbstractSimpleUnirestRunnerMixin<ISCDastSessionData> {
    @Inject @ReflectiveAccess @Getter private SCDastSessionDataManager sessionDataManager;
    
    @Override
    protected final void configure(UnirestInstance unirest, ISCDastSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getScDastUrlConfig());
        ProxyHelper.configureProxy(unirest, "sc-dast", sessionData.getScDastUrlConfig().getUrl());
        unirest.config().requestCompression(false); // TODO Check whether SC DAST suffers from the same issue as SSC, with some requests failing if compression is enabled
        unirest.config().addDefaultHeader("Authorization", "FortifyToken "+new String(sessionData.getActiveToken()));
    }
    
    @Override
    protected final ISCDastSessionData getSessionData(String sessionName) {
        return sessionDataManager.get(sessionName, true);
    }
}
