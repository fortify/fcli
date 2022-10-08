package com.fortify.cli.sc_dast.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.rest.runner.IUnirestRunner;
import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_dast.session.manager.ISCDastSessionData;
import com.fortify.cli.sc_dast.session.manager.SCDastSessionDataFromEnv;
import com.fortify.cli.sc_dast.session.manager.SCDastSessionDataManager;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@ReflectiveAccess @FixInjection
public class SCDastUnirestRunnerMixin implements IUnirestRunner {
    @Inject private UnirestRunner unirestRunner;
    @Inject private SCDastSessionDataManager sessionDataManager;
    @Inject private SSCTokenHelper tokenHelper;
    @Mixin private SessionNameMixin.OptionalOption sessionNameMixin;
    
    @Override
    public <R> R run(Function<UnirestInstance, R> f) {
        return unirestRunner.run(unirest->run(unirest, f));
    }
    
    private <R> R run(UnirestInstance unirest, Function<UnirestInstance, R> f) {
        ISCDastSessionData sessionData = getSessionData();
        configureSCDast(unirest, sessionData);
        try {
            return f.apply(unirest);
        } finally {
            cleanup(sessionData);
        }
    }
    
    private void configureSCDast(UnirestInstance unirest, ISCDastSessionData sessionData) {
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
    private ISCDastSessionData getSessionData() {
        SCDastSessionDataFromEnv sessionDataFromEnv = new SCDastSessionDataFromEnv(tokenHelper);
        return !sessionNameMixin.hasSessionName() && sessionDataFromEnv.hasConfigFromEnv()
                ? sessionDataFromEnv
                : sessionDataManager.get(sessionNameMixin.getSessionName(), true);
    }
    
    private void cleanup(ISCDastSessionData sessionData) {
        if ( sessionData instanceof SCDastSessionDataFromEnv ) {
            ((SCDastSessionDataFromEnv)sessionData).cleanup();
        }
    }
}
