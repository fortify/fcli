package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.util.FixInjection;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@FixInjection
public abstract class AbstractUnirestRunnerWithSessionDataMixin<D extends ISessionData> extends AbstractUnirestRunnerMixin {
    @Mixin private SessionNameMixin.OptionalOption sessionNameMixin;
    
    @Override
    protected final void configure(UnirestInstance unirest) {
        configure(unirest, getSessionData());
    }
    
    protected abstract void configure(UnirestInstance unirest, D sessionData);

    public final D getSessionData() {
        return getSessionData(sessionNameMixin.getSessionName());
    }
    
    protected abstract D getSessionData(String sessionName);
}
