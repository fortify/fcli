package com.fortify.cli.sc_sast.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.util.FixInjection;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;

@ReflectiveAccess @FixInjection
public class SCSastControllerUnirestRunnerMixin extends AbstractSCSastUnirestRunnerMixin {
    @Override
    public <R> R run(Function<UnirestInstance, R> f) {
        return runOnController(f);
    }
}
