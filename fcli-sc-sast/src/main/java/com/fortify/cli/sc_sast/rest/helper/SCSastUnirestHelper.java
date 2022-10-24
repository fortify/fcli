package com.fortify.cli.sc_sast.rest.helper;

import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;

import kong.unirest.UnirestInstance;

public class SCSastUnirestHelper {
    public static final void configureScSastControllerUnirestInstance(UnirestInstance unirest, SCSastSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getScSastUrlConfig());
        unirest.config().setDefaultHeader("fortify-client", String.valueOf(sessionData.getScSastClientAuthToken()));
    }
    
    public static final void configureSscUnirestInstance(UnirestInstance unirest, SCSastSessionData sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getSscUrlConfig());
        unirest.config().requestCompression(false); // For some reason, larger SSC requests fail when compression is enabled
        unirest.config().addDefaultHeader("Authorization", "FortifyToken "+new String(sessionData.getActiveSSCToken()));
    }
}
