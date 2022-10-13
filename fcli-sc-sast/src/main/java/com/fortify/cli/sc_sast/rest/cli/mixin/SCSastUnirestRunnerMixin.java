package com.fortify.cli.sc_sast.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.rest.runner.IUnirestRunner;
import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public class SCSastUnirestRunnerMixin implements IUnirestRunner {
    @Inject private UnirestRunner runner;
    @Inject private SCSastSessionDataManager sessionDataManager;
    @Mixin private SessionNameMixin.OptionalOption sessionNameMixin;
    
    public <R> R run(Function<UnirestInstance, R> f) {
        return runner.run(unirest->run(unirest, f));
    }
    
    private <R> R run(UnirestInstance unirest, Function<UnirestInstance, R> f) {
        configure(unirest);
        try {
            return f.apply(unirest);
        } finally {
            cleanup(unirest);
        }
    }

    private void configure(UnirestInstance unirest) {
        SCSastSessionData sessionData = sessionDataManager.get(sessionNameMixin.getSessionName(), true);
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getUrlConfig());
        unirest.config().setDefaultHeader("fortify-client", String.valueOf(sessionData.getClientAuthToken()));
    }
    
    private void cleanup(UnirestInstance unirest) {
        // TODO Once we implement automated login based on env vars, we can clean up any generated tokens here if necessary
    }
    
    
}
