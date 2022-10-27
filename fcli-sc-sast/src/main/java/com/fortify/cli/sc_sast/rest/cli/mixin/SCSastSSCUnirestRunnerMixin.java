package com.fortify.cli.sc_sast.rest.cli.mixin;

import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_sast.rest.helper.SCSastUnirestHelper;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;

@ReflectiveAccess @FixInjection
public class SCSastSSCUnirestRunnerMixin extends AbstractSCSastUnirestRunnerMixin {
    @Override
    protected final void configure(UnirestInstance unirest, SCSastSessionData sessionData) {
        SCSastUnirestHelper.configureScSastControllerUnirestInstance(unirest, sessionData);
    }
}
