/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_sast._common.rest.helper;

import com.fortify.cli.common.http.connection.helper.ConnectionHelper;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.sc_sast._common.session.helper.SCSastSessionDescriptor;

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
        ConnectionHelper.configureTimeouts(unirest, "sc-sast");
        unirest.config().requestCompression(false); // For some reason, larger SSC requests fail when compression is enabled
        unirest.config().setDefaultHeader("Authorization", "FortifyToken "+new String(sessionData.getActiveSSCToken()));
    }
}
