package com.fortify.cli.ssc.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
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
    @Inject private UnirestRunner runner;
    @Inject private SSCSessionDataManager sessionDataManager;
    @Inject private SSCTokenHelper tokenHelper;
    @Mixin private SessionNameMixin.OptionalOption sessionNameMixin;
    
    public <R> R run(Function<UnirestInstance, R> f) {
        return runner.run(unirest->run(unirest, f));
    }
    
    private <R> R run(UnirestInstance unirest, Function<UnirestInstance, R> f) {
        ISSCSessionData sessionData = getSessionData();
        configure(unirest, sessionData);
        try {
            return f.apply(unirest);
        } finally {
            cleanup(unirest, sessionData);
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

    private void configure(UnirestInstance unirest, ISSCSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getUrlConfig());
        unirest.config().requestCompression(false); // TODO For some reason, in native binaries, compression may cause issues
        unirest.config().addDefaultHeader("Authorization", "FortifyToken "+new String(sessionData.getActiveToken()));
    }
    
    private void cleanup(UnirestInstance unirest, ISSCSessionData sessionData) {
        if ( sessionData instanceof SSCSessionDataFromEnv ) {
            ((SSCSessionDataFromEnv)sessionData).cleanup();
        }
    }
}
