package com.fortify.cli.sc_sast.rest.helper;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.sc_sast.session.helper.SCSastSessionDescriptor;

import kong.unirest.UnirestInstance;

public class SCSastUnirestHelper {
    public static final void configureScSastControllerUnirestInstance(UnirestInstance unirest, SCSastSessionDescriptor sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getScSastUrlConfig());
        ProxyHelper.configureProxy(unirest, "sc-sast", sessionData.getScSastUrlConfig().getUrl());
        unirest.config().setDefaultHeader("fortify-client", String.valueOf(sessionData.getScSastClientAuthToken()));
    }
    
    public static final void configureSscUnirestInstance(UnirestInstance unirest, SCSastSessionDescriptor sessionData) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionData.getSscUrlConfig());
        ProxyHelper.configureProxy(unirest, "sc-sast", sessionData.getSscUrlConfig().getUrl());
        unirest.config().requestCompression(false); // For some reason, larger SSC requests fail when compression is enabled
        unirest.config().addDefaultHeader("Authorization", "FortifyToken "+new String(sessionData.getActiveSSCToken()));
    }
}
