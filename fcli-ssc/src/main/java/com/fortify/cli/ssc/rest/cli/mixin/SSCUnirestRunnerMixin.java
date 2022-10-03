package com.fortify.cli.ssc.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.ssc.session.manager.ISSCSessionData;
import com.fortify.cli.ssc.session.manager.SSCSessionDataFromEnv;
import com.fortify.cli.ssc.session.manager.SSCSessionDataManager;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public class SSCUnirestRunnerMixin {
    @Inject private SSCSessionDataManager sessionDataManager;
    @Inject private SSCTokenHelper tokenHelper;
    @Mixin private SessionNameMixin.OptionalOption sessionNameMixin;
    
    public <R> R run(Function<UnirestInstance, R> f) {
        ISSCSessionData sessionData = getSessionData();
        try {
            return tokenHelper.run(sessionData.getUrlConfig(), sessionData.getActiveToken(), f);
        } finally {
            cleanup(sessionData);
        }
    }

    /**
     * Return an ISSCSessionData instance. If no session name has been explicitly specified,
     * and session data is available from the environment, this will return an 
     * {@link SSCSessionDataFromEnv} instance, otherwise {@link SSCSessionDataManager}
     * will be used to retrieve a previously persisted session.
     * @return
     */
    private ISSCSessionData getSessionData() {
        SSCSessionDataFromEnv sessionDataFromEnv = new SSCSessionDataFromEnv(tokenHelper);
        return !sessionNameMixin.hasSessionName() && sessionDataFromEnv.hasConfigFromEnv()
                ? sessionDataFromEnv
                : sessionDataManager.get(sessionNameMixin.getSessionName(), true);
    }
    
    private void cleanup(ISSCSessionData sessionData) {
        if ( sessionData instanceof SSCSessionDataFromEnv ) {
            ((SSCSessionDataFromEnv)sessionData).cleanup();
        }
    }
}
