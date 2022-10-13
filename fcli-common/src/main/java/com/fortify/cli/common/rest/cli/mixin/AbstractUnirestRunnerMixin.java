package com.fortify.cli.common.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.rest.runner.IUnirestRunner;
import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.spi.ISessionDataManager;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.common.variable.IMinusVariableNamePrefixSupplier;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess @FixInjection
public abstract class AbstractUnirestRunnerMixin<D extends ISessionData,M extends ISessionDataManager<? extends D>> implements IUnirestRunner, IMinusVariableNamePrefixSupplier {
    @Inject private UnirestRunner runner;
    @Getter @Mixin private SessionNameMixin.OptionalOption sessionNameMixin;
    
    @Override
    public final <R> R run(Function<UnirestInstance, R> f) {
        return runner.run(unirest->run(unirest, f));
    }
    
    @Override
    public final String getMinusVariableNamePrefix() {
        return getSessionDataManager().getMinusVariableNamePrefix(sessionNameMixin.getSessionName());
    }
    
    private final <R> R run(UnirestInstance unirest, Function<UnirestInstance, R> f) {
        D sessionData = getSessionData();
        configure(unirest, sessionData);
        try {
            return f.apply(unirest);
        } finally {
            cleanup(unirest, sessionData);
        }
    }
    
    protected abstract D getSessionData();
    protected abstract M getSessionDataManager();
    protected abstract void configure(UnirestInstance unirest, D sessionData);
    protected abstract void cleanup(UnirestInstance unirest, D sessionData);
}
