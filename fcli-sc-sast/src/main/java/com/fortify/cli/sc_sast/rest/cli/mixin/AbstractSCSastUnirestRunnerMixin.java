package com.fortify.cli.sc_sast.rest.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.AbstractUnirestRunnerMixin;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@ReflectiveAccess @FixInjection
public abstract class AbstractSCSastUnirestRunnerMixin extends AbstractUnirestRunnerMixin<SCSastSessionData, SCSastSessionDataManager> {
    @Getter @Inject private SCSastSessionDataManager sessionDataManager;
    
    @Override
    protected final SCSastSessionData getSessionData() {
        return sessionDataManager.get(getSessionNameMixin().getSessionName(), true);
    }
    
    @Override
    protected final void cleanup(UnirestInstance unirest, SCSastSessionData sessionData) {
        // Nothing to do
    }
}
