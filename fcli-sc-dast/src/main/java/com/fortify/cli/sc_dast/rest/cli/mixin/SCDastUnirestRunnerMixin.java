package com.fortify.cli.sc_dast.rest.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.AbstractUnirestRunnerMixin;
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

@ReflectiveAccess @FixInjection
public class SCDastUnirestRunnerMixin extends AbstractUnirestRunnerMixin<ISCDastSessionData, SCDastSessionDataManager> {
    @Getter @Inject private SCDastSessionDataManager sessionDataManager;
    
    @Override
    protected final void configure(UnirestInstance unirest, ISCDastSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getScDastUrlConfig());
        unirest.config().requestCompression(false); // TODO Check whether SC DAST suffers from the same issue as SSC, with some requests failing if compression is enabled
        unirest.config().addDefaultHeader("Authorization", "FortifyToken "+new String(sessionData.getActiveToken()));
    }
    
    /**
     * Return an ISCDastSessionData instance. If no session name has been explicitly specified,
     * and session data is available from the environment, this will return an 
     * {@link SCDastSessionDataFromEnv} instance, otherwise {@link SCDastSessionDataManager}
     * will be used to retrieve a previously persisted session.
     * @return
     */
    @Override
    protected final ISCDastSessionData getSessionData() {
        return sessionDataManager.get(getSessionNameMixin().getSessionName(), true);
    }
    
    @Override
    protected final void cleanup(UnirestInstance unirest, ISCDastSessionData sessionData) {}
}
