package com.fortify.cli.ssc.rest.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.AbstractUnirestRunnerMixin;
import com.fortify.cli.ssc.session.manager.ISSCSessionData;
import com.fortify.cli.ssc.session.manager.SSCSessionDataManager;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@ReflectiveAccess
public class SSCUnirestRunnerMixin extends AbstractUnirestRunnerMixin<ISSCSessionData, SSCSessionDataManager> {
    @Getter @Inject private SSCSessionDataManager sessionDataManager;
    @Inject private SSCTokenHelper tokenHelper;

    /**
     * Return an ISSCSessionData instance. If no session name has been explicitly specified,
     * and session data is available from the environment, this will return an 
     * {@link SSCSessionDataFromEnv} instance, otherwise {@link SSCSessionDataManager}
     * will be used to retrieve a previously persisted session.
     * @return
     */
    @Override
    protected final ISSCSessionData getSessionData() {
        return sessionDataManager.get(getSessionNameMixin().getSessionName(), true);
    }
    
    @Override
    protected void configure(UnirestInstance unirest, ISSCSessionData sessionData) {
        SSCTokenHelper.configureUnirest(unirest, sessionData.getUrlConfig(), sessionData.getActiveToken());
    }
    
    @Override
    protected final void cleanup(UnirestInstance unirest, ISSCSessionData sessionData) {}
}
