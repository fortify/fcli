package com.fortify.cli.sc_sast.rest.cli.mixin;

import java.util.function.BiFunction;

import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;

import kong.unirest.UnirestInstance;

@FixInjection
public class SCSastControllerUnirestRunnerMixin extends AbstractSCSastUnirestRunnerMixin {
    @Override
    public <R> R run(BiFunction<UnirestInstance, SCSastSessionData, R> f) {
        return runOnController(f);
    }
}
