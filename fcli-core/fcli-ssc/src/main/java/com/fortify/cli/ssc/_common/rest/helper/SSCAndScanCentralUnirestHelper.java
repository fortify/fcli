/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.ssc._common.rest.helper;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;

import kong.unirest.UnirestInstance;

public class SSCAndScanCentralUnirestHelper {
    public static final void configureSscUnirestInstance(UnirestInstance unirest, SSCAndScanCentralSessionDescriptor sessionDescriptor) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionDescriptor.getSscUrlConfig());
        ProxyHelper.configureProxy(unirest, "sc-sast", sessionDescriptor.getSscUrlConfig().getUrl());
        unirest.config().requestCompression(false); // For some reason, larger SSC requests fail when compression is enabled
        unirest.config().setDefaultHeader("Authorization", "FortifyToken "+new String(sessionDescriptor.getActiveSSCToken()));
    }
    
    public static final void configureScSastControllerUnirestInstance(UnirestInstance unirest, SSCAndScanCentralSessionDescriptor sessionDescriptor) {
        checkEnabled("SC-SAST", sessionDescriptor.getScSastDisabledReason());
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionDescriptor.getScSastUrlConfig());
        ProxyHelper.configureProxy(unirest, "sc-sast", sessionDescriptor.getScSastUrlConfig().getUrl());
        unirest.config().setDefaultHeader("fortify-client", String.valueOf(sessionDescriptor.getScSastClientAuthToken()));
    }
    
    public static final void configureScDastControllerUnirestInstance(UnirestInstance unirest, SSCAndScanCentralSessionDescriptor sessionDescriptor) {
        checkEnabled("SC-DAST", sessionDescriptor.getScDastDisabledReason());
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionDescriptor.getScDastUrlConfig());
        ProxyHelper.configureProxy(unirest, "sc-dast", sessionDescriptor.getScDastUrlConfig().getUrl());
        unirest.config().requestCompression(false); // TODO Check whether SC DAST suffers from the same issue as SSC, with some requests failing if compression is enabled
        unirest.config().setDefaultHeader("Authorization", "FortifyToken "+new String(sessionDescriptor.getActiveSSCToken()));
    }

    private static final void checkEnabled(String type, String disabledReason) {
        if ( StringUtils.isNotBlank(disabledReason) ) {
            throw new FcliSimpleException("Can't connect to %s: %s", type, disabledReason);
        }
    }
}
