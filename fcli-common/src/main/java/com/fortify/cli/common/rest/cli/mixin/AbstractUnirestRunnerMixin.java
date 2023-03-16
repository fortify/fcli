package com.fortify.cli.common.rest.cli.mixin;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.fortify.cli.common.rest.runner.GenericUnirestFactory;
import com.fortify.cli.common.rest.runner.IUnirestWithSessionDataRunner;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.util.FixInjection;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@FixInjection
public abstract class AbstractUnirestRunnerMixin<D extends ISessionData> implements IUnirestWithSessionDataRunner<D> {
    @Inject @ReflectiveAccess private GenericUnirestFactory genericUnirestFactory;
    @Mixin private SessionNameMixin.OptionalOption sessionNameMixin;
    
    protected final <R> R run(BiConsumer<UnirestInstance, D> configurer, BiFunction<UnirestInstance, D, R> f) {
        if ( f == null ) { throw new IllegalStateException("Function may not be null"); }
        D sessionData = getSessionData();
        try ( var unirest = genericUnirestFactory.createUnirestInstance() ) {
            configurer.accept(unirest, sessionData);
            return f.apply(unirest, sessionData);
        }
    }
    
    public D getSessionData() {
        return getSessionData(sessionNameMixin.getSessionName());
    }
    
    protected abstract D getSessionData(String sessionName);
}
