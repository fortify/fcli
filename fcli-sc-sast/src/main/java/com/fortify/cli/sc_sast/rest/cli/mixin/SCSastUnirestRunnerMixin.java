package com.fortify.cli.sc_sast.rest.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.AbstractUnirestRunnerMixin;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@ReflectiveAccess @FixInjection
public class SCSastUnirestRunnerMixin extends AbstractUnirestRunnerMixin<SCSastSessionData, SCSastSessionDataManager> {
    @Getter @Inject private SCSastSessionDataManager sessionDataManager;
    
    @Override
    protected SCSastSessionData getSessionData() {
        return sessionDataManager.get(getSessionNameMixin().getSessionName(), true);
    }

    @Override
    protected final void configure(UnirestInstance unirest, SCSastSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getUrlConfig());
        unirest.config().setDefaultHeader("fortify-client", String.valueOf(sessionData.getClientAuthToken()));
    }
    
    @Override
    protected final void cleanup(UnirestInstance unirest, SCSastSessionData sessionData) {
        // TODO Once we implement automated login based on env vars, we can clean up any generated tokens here if necessary
    }
}
