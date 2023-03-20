package com.fortify.cli.common.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.rest.runner.GenericUnirestFactory;
import com.fortify.cli.common.rest.runner.IUnirestRunner;
import com.fortify.cli.common.util.FixInjection;

import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Setter;

@FixInjection
public abstract class AbstractUnirestRunnerMixin implements IUnirestRunner {
    @Setter(onMethod=@__({@Inject})) private GenericUnirestFactory genericUnirestFactory;
    
    @Override
    public final <R> R run(Function<UnirestInstance, R> f) {
        if ( f == null ) { throw new IllegalStateException("Function may not be null"); }
        try ( var unirest = genericUnirestFactory.createUnirestInstance() ) {
            configure(unirest);
            return f.apply(unirest);
        }
    }

    protected abstract void configure(UnirestInstance unirest);
}
