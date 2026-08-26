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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.fod._common.session.helper.oauth.IFoDUserCredentials;

import kong.unirest.UnirestInstance;

/**
 * Helper class for requesting Multi-Factor Authentication (MFA) codes in Fortify on Demand (FoD). 
 * @author Sangamesh Vijaykumar
 */
public class FoDMfaHelper {
    
    public static final void requestMfaCode(IUrlConfig urlConfig, IFoDUserCredentials userCredentials, FoDMfaDeliveryType deliveryType) {
        try ( var unirest = UnirestHelper.createUnirestInstance() ) {
            configureUnirest(unirest, urlConfig);
            
            ObjectNode requestBody = JsonHelper.getObjectMapper().createObjectNode();
            requestBody.put("multiFactorAuthorizationType", deliveryType.getApiValue());
            requestBody.put("username", String.format("%s\\%s", userCredentials.getTenant(), userCredentials.getUser()));
            requestBody.put("password", String.valueOf(userCredentials.getPassword()));
            
            unirest.post("/api/v3/multi-factor-authorization-code")
                    .headerReplace(HttpHeader.ACCEPT, "application/json")
                    .headerReplace(HttpHeader.CONTENT_TYPE, "application/json")
                    .body(requestBody)
                    .asEmpty();
        } catch ( UnexpectedHttpResponseException e ) {
            if ( e.getStatus() == 400 ) {
                throw new FcliSimpleException(
                    "MFA is not enabled for this tenant, or the provided credentials are invalid."
                    + " Contact your FoD administrator, then try again."
                );
            } else if ( e.getStatus() == 401 || e.getStatus() == 403 ) {
                throw new FcliSimpleException(
                    "Authentication failed: invalid username, tenant, or password."
                );
            }
            throw e;
        }
    }

    private static void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, "fod", urlConfig.getUrl());
        UnirestJsonHeaderConfigurer.configure(unirest);
    }
}
