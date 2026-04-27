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
package com.fortify.cli.fod._common.session.helper;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.session.helper.oauth.FoDTokenCreateResponse;

import kong.unirest.UnirestInstance;

public class FoDSessionValidationHelper {
    public static record SessionStatus(boolean valid, FoDTokenCreateResponse tokenData) {}

    public static SessionStatus checkTokenStatus(IUrlConfig urlConfig, String accessToken) {
        try ( UnirestInstance unirest = UnirestHelper.createUnirestInstance() ) {
            UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
            UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
            ProxyHelper.configureProxy(unirest, "fod", urlConfig.getUrl());
            UnirestJsonHeaderConfigurer.configure(unirest);
            unirest.config().setDefaultHeader("Authorization", "Bearer " + accessToken);
            try {
                unirest.get(FoDUrls.LOOKUP_ITEMS).queryString("limit", 1).asString();
                var tokenData = new FoDTokenCreateResponse();
                tokenData.setAccessToken(accessToken);
                return new SessionStatus(true, tokenData);
            } catch ( UnexpectedHttpResponseException e ) {
                if ( e.getStatus()==401 ) {
                    return new SessionStatus(false, null);
                } else {
                    throw new FcliSimpleException("Unable to verify session status: FoD returned HTTP "+e.getStatus(), e);
                }
            }
        } catch ( FcliSimpleException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new FcliSimpleException("Unable to connect to FoD to verify session status", e);
        }
    }
}
