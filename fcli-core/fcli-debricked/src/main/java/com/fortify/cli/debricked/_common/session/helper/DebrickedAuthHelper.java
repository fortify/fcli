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
	public static final void configureAdHocUnirestInstance(UnirestInstance unirest, IDebrickedLoginOptions debrickedLoginOptions) {
		String debrickedJwtToken = getJwtTokenResponse(unirest, debrickedLoginOptions).getToken();
		UnirestJsonHeaderConfigurer.configure(unirest); // This must be done after getJwtTokenResponse
        String authHeader = String.format("Bearer %s", debrickedJwtToken);
        unirest.config().setDefaultHeader("Authorization", authHeader);
	}
	
	public static final DebrickedJwtTokenResponse getJwtTokenResponse(UnirestInstance unirest, IUrlConfig urlConfig, String refreshToken) {
        configureNonAuthenticatedUnirestInstance(unirest, urlConfig);
        return authenticate(unirest, refreshToken);
	}
	
	public static final DebrickedJwtTokenResponse getJwtTokenResponse(UnirestInstance unirest, IDebrickedLoginOptions debrickedLoginOptions) {
	    configureNonAuthenticatedUnirestInstance(unirest, debrickedLoginOptions.getUrlConfigOptions());
        IDebrickedAuthOptions authOptions = debrickedLoginOptions.getAuthOptions();
        IDebrickedUserCredentialOptions userCredentialsOptions = authOptions.getUserCredentialsOptions();
        IDebrickedAccessTokenCredentialOptions tokenOptions = authOptions.getTokenOptions();
        if ( userCredentialsOptions!=null && StringUtils.isNotBlank(userCredentialsOptions.getUser()) ) {
            return authenticate(unirest, userCredentialsOptions);
        } else if ( tokenOptions!=null && tokenOptions.getAccessToken()!=null ) {
            return authenticate(unirest, new String(tokenOptions.getAccessToken()));
        } else {
            throw new FcliSimpleException("Either Debricked user credentials or access token need to be specified");
        }
    }
    
    private static final DebrickedJwtTokenResponse authenticate(UnirestInstance debrickedUnirest, String accessOrRefreshToken) {
        return debrickedUnirest.post("/api/login_refresh")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("refresh_token", accessOrRefreshToken)
                .asObject(DebrickedJwtTokenResponse.class)
                .getBody();
    }

    private static final DebrickedJwtTokenResponse authenticate(UnirestInstance debrickedUnirest, IDebrickedUserCredentialOptions userCredentialsOptions) {
        return debrickedUnirest.post("/api/login_check")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.field("_username", userCredentialsOptions.getUser())
				.field("_password", new String(userCredentialsOptions.getPassword()))
				.asObject(DebrickedJwtTokenResponse.class)
				.getBody();
    }
    
    private static final void configureNonAuthenticatedUnirestInstance(UnirestInstance unirest, IUrlConfig urlConfig) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, "debricked", urlConfig.getUrl());
    }
}
