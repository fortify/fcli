package com.fortify.cli.common.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.util.FixInjection;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;

@ReflectiveAccess @FixInjection
public abstract class AbstractSimpleUnirestRunnerMixin<D extends ISessionData> extends AbstractUnirestRunnerMixin<D> {
    @Override
    public final <R> R run(Function<UnirestInstance, R> f) {
        return run(this::configure, f);
    }
    
    protected abstract void configure(UnirestInstance unirest, D sessionData);
}
