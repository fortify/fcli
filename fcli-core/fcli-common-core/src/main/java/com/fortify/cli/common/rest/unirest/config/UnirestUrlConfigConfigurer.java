/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.rest.unirest.config;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;

import kong.unirest.UnirestInstance;

public final class UnirestUrlConfigConfigurer {
    /**
     * Configure the given {@link UnirestInstance} based on the given {@link IUrlConfig} instance
     * @param unirestInstance {@link UnirestInstance} to be configured
     * @param urlConfig used to configure the {@link UnirestInstance}
     */
    // TODO Should we use FcliTechnicalException or FcliBugException?
    public static final void configure(UnirestInstance unirestInstance, IUrlConfig urlConfig) {
        if ( unirestInstance==null ) { throw new FcliTechnicalException("Unirest instance may not be null"); }
        if ( urlConfig==null ) { throw new FcliTechnicalException("URL configuration may not be null"); }
        unirestInstance.config()
            .defaultBaseUrl(normalizeUrl(urlConfig.getUrl()))
            .verifySsl(!urlConfig.isInsecureModeEnabled())
            .socketTimeout(urlConfig.getSocketTimeoutInMillis())
            .connectTimeout(urlConfig.getConnectTimeoutInMillis());
        applyHeaders(unirestInstance, urlConfig.getHeaders());
    }
    
    /**
     * Configure the given {@link UnirestInstance} based on the {@link IUrlConfig} returned 
     * by the given {@link IUrlConfigSupplier} instance.
     * @param unirestInstance {@link UnirestInstance} to be configured
     * @param urlConfigSupplier used to configure the {@link UnirestInstance}
     */
    // TODO Should we use FcliTechnicalException or FcliBugException?
    public static final void configure(UnirestInstance unirestInstance, IUrlConfigSupplier urlConfigSupplier) {
        if ( urlConfigSupplier==null ) { throw new FcliTechnicalException("URL configuration provider may not be null"); }
        configure(unirestInstance, urlConfigSupplier.getUrlConfig());
    }
    
    private static final String normalizeUrl(String url) {
        // We remove any trailing slashes, assuming that most users will specify relative URL's starting with /
        return url.replaceAll("/+$", "");
    }

    private static void applyHeaders(UnirestInstance unirestInstance, List<String> headers) {
        if ( headers==null ) {
            return;
        }
        for ( String header : headers ) {
            applyHeader(unirestInstance, header);
        }
    }

    private static void applyHeader(UnirestInstance unirestInstance, String header) {
        if ( StringUtils.isBlank(header) ) {
            throw new FcliSimpleException("HTTP header values must not be blank");
        }
        int separatorIndex = header.indexOf(':');
        if ( separatorIndex < 1 ) {
            throw new FcliSimpleException("Invalid HTTP header '%s'; expected NAME: VALUE", header);
        }
        String name = header.substring(0, separatorIndex).trim();
        String value = header.substring(separatorIndex + 1).trim();
        if ( StringUtils.isBlank(name) ) {
            throw new FcliSimpleException("Invalid HTTP header '%s'; header name must not be blank", header);
        }
        unirestInstance.config().setDefaultHeader(name, value);
    }
}
