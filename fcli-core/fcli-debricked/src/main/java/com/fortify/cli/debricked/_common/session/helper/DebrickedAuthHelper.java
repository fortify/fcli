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
package com.fortify.cli.debricked._common.session.helper;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.debricked._common.session.helper.IDebrickedLoginOptions.IDebrickedAccessTokenCredentialOptions;
import com.fortify.cli.debricked._common.session.helper.IDebrickedLoginOptions.IDebrickedAuthOptions;
import com.fortify.cli.debricked._common.session.helper.IDebrickedLoginOptions.IDebrickedUserCredentialOptions;

import kong.unirest.UnirestInstance;
public final class DebrickedAuthHelper  {

	public static final void configureAuthenticatedUnirest(UnirestInstance unirest, IDebrickedLoginOptions debrickedLoginOptions) {
	    configureNonAuthenticatedUnirest(unirest, debrickedLoginOptions.getUrlConfigOptions());
		String debrickedJwtToken = getJwtToken(unirest, debrickedLoginOptions);
        String authHeader = String.format("Bearer %s", debrickedJwtToken);
        unirest.config().setDefaultHeader("Authorization", authHeader);
        UnirestJsonHeaderConfigurer.configure(unirest);
	}
	
	public static final void configureNonAuthenticatedUnirest(UnirestInstance unirest, IUrlConfig urlConfig) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, "fod", urlConfig.getUrl());
    }

	public static final String getJwtToken(UnirestInstance debrickedUnirest, IDebrickedLoginOptions debrickedLoginOptions) {
		return getJwtTokenResponse(debrickedUnirest, debrickedLoginOptions).getToken();
	}
	
	public static final DebrickedJwtTokenResponse getJwtTokenResponse(UnirestInstance debrickedUnirest, IDebrickedLoginOptions debrickedLoginOptions) {
        IDebrickedAuthOptions authOptions = debrickedLoginOptions.getAuthOptions();
        IDebrickedUserCredentialOptions userCredentialsOptions = authOptions.getUserCredentialsOptions();
        IDebrickedAccessTokenCredentialOptions tokenOptions = authOptions.getTokenOptions();
        if ( userCredentialsOptions!=null && StringUtils.isNotBlank(userCredentialsOptions.getUser()) ) {
            return getJwtTokenResponse(debrickedUnirest, userCredentialsOptions);
        } else if ( tokenOptions!=null && tokenOptions.getAccessToken()!=null ) {
            return getJwtTokenResponse(debrickedUnirest, tokenOptions);
        } else {
            throw new FcliSimpleException("Either Debricked user credentials or access token need to be specified");
        }
    }

    private static DebrickedJwtTokenResponse getJwtTokenResponse(UnirestInstance debrickedUnirest, IDebrickedAccessTokenCredentialOptions tokenOptions) {
        return debrickedUnirest.post("/api/login_refresh")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.field("refresh_token", new String(tokenOptions.getAccessToken()))
				.asObject(DebrickedJwtTokenResponse.class)
				.getBody();
    }

    private static DebrickedJwtTokenResponse getJwtTokenResponse(UnirestInstance debrickedUnirest, IDebrickedUserCredentialOptions userCredentialsOptions) {
        return debrickedUnirest.post("/api/login_check")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.field("_username", userCredentialsOptions.getUser())
				.field("_password", new String(userCredentialsOptions.getPassword()))
				.asObject(DebrickedJwtTokenResponse.class)
				.getBody();
    }
}
