package com.fortify.cli.sc_sast.rest.cli.mixin;

import java.util.function.BiFunction;

import com.fortify.cli.common.rest.cli.mixin.AbstractUnirestRunnerMixin;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_sast.rest.helper.SCSastUnirestHelper;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@FixInjection
public abstract class AbstractSCSastUnirestRunnerMixin extends AbstractUnirestRunnerMixin<SCSastSessionData> {
    @Inject @ReflectiveAccess @Getter private SCSastSessionDataManager sessionDataManager;
    
    @Override
    protected final SCSastSessionData getSessionData(String sessionName) {
        return sessionDataManager.get(sessionName, true);
    }
    
    public final <R> R runOnSSC(BiFunction<UnirestInstance, SCSastSessionData, R> f) {
        return run(SCSastUnirestHelper::configureSscUnirestInstance, f);
    }
    
    public final <R> R runOnController(BiFunction<UnirestInstance, SCSastSessionData, R> f) {
        return run(SCSastUnirestHelper::configureScSastControllerUnirestInstance, f);
    }
}
