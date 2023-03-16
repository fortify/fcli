package com.fortify.cli.common.rest.cli.mixin;

import java.util.function.BiFunction;

import com.fortify.cli.common.session.manager.api.ISessionData;

import kong.unirest.UnirestInstance;

public abstract class AbstractSimpleUnirestRunnerMixin<D extends ISessionData> extends AbstractUnirestRunnerMixin<D> {
    @Override
    public final <R> R run(BiFunction<UnirestInstance, D, R> f) {
        return run(this::configure, f);
    }
    
    protected abstract void configure(UnirestInstance unirest, D sessionData);
}
