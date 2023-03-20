package com.fortify.cli.sc_sast.rest.cli.mixin;

import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_sast.rest.helper.SCSastUnirestHelper;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;

import kong.unirest.UnirestInstance;

@FixInjection
public class SCSastSSCUnirestRunnerMixin extends AbstractSCSastUnirestRunnerMixin {
    @Override
    protected void configure(UnirestInstance unirest, SCSastSessionData sessionData) {
        SCSastUnirestHelper.configureSscUnirestInstance(unirest, sessionData);
    }
}
