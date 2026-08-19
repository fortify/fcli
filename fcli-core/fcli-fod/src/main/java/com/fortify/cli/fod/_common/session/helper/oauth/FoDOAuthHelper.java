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
package com.fortify.cli.fod._common.session.helper.oauth;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.fod._common.session.cli.mixin.FoDSessionLoginOptions;

import kong.unirest.UnirestInstance;

// TODO Consider moving all classes in this package to a more appropriate package,
//      for example as a sub-package of the 'rest' package.
public class FoDOAuthHelper {
    private static final String MFA_GUIDANCE = 
        "If MFA is required, provide the security code:\n" +
        "  --code <code>  (or -c <code>) to provide the security code\n" +
        "  --totp          to indicate the code is from a TOTP authenticator app";
    
    private static final String ERROR_WITH_CODE = 
        "Authentication failed. Possible causes:\n" +
        "  - Incorrect username or password\n" +
        "  - MFA/TOTP code incorrect, expired, or wrong type (TOTP vs MFA)\n" +
        "Please verify your credentials and MFA/TOTP code if applicable:\n" +
        MFA_GUIDANCE;
    
    private static final String ERROR_WITHOUT_CODE =
        "Authentication failed. Possible causes:\n" +
        "  - Incorrect username or password\n" +
        "  - FoD tenant requires MFA/TOTP authentication\n\n" +
        MFA_GUIDANCE;

    public static final FoDTokenCreateResponse createToken(IUrlConfig urlConfig, IFoDUserCredentials uc, String... scopes) {
        Map<String,Object> formData = generateTokenRequest(uc, IFoDUserAuthCode.NONE, scopes);
    try ( var unirest = UnirestHelper.createUnirestInstance() ) {
            return createToken(unirest, urlConfig, formData);
        }
    }

    public static final FoDTokenCreateResponse createToken(IUrlConfig urlConfig, IFoDUserCredentials uc, IFoDUserAuthCode authCode, String... scopes) {
        Map<String,Object> formData = generateTokenRequest(uc, authCode, scopes);
    try ( var unirest = UnirestHelper.createUnirestInstance() ) {
            return createToken(unirest, urlConfig, formData);
        }
    }

    public static final FoDTokenCreateResponse createToken(IUrlConfig urlConfig, IFoDClientCredentials cc, String... scopes) {
        Map<String,Object> formData = generateTokenRequest(cc, scopes);
    try ( var unirest = UnirestHelper.createUnirestInstance() ) {
            return createToken(unirest, urlConfig, formData);
        }
    }

    public static final FoDTokenCreateResponse createUserToken(IUrlConfig urlConfig, FoDSessionLoginOptions loginOptions) {
        var credBuilder = BasicFoDUserCredentials.builder()
                .tenant(loginOptions.getUserCredentialOptions().getTenant())
                .user(loginOptions.getUserCredentialOptions().getUser())
                .password(loginOptions.getUserCredentialOptions().getPassword());
                
        var authCodeBuilder = BasicFoDUserAuthCode.builder()
                .securityCode(loginOptions.getUserCredentialOptions().getSecurityCode())
                .isTotp(loginOptions.getUserCredentialOptions().isTotp());

        try {
            return createToken(urlConfig, credBuilder.build(), authCodeBuilder.build(), loginOptions.getAuthOptions().getScopes());
        } catch (UnexpectedHttpResponseException e) {
            if (e.getStatus() == 400) {
                String errorMessage = loginOptions.hasSecurityCode() ? ERROR_WITH_CODE : ERROR_WITHOUT_CODE;
                throw new FcliSimpleException(errorMessage);
            }
            throw new FcliTechnicalException(e.getMessage(), e);
        }
    }
    
    private static final FoDTokenCreateResponse createToken(UnirestInstance unirest, IUrlConfig urlConfig, Map<String, Object> formData) {
        configureUnirest(unirest, urlConfig);
        return unirest.post("/oauth/token")
                // Use headerReplace to replace rather than add headers (avoid duplicates with defaults)
                .headerReplace(HttpHeader.ACCEPT, "application/json")
                .headerReplace(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .fields(formData)
                .asObject(FoDTokenCreateResponse.class)
                .getBody();
    }
    
    private static final void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, "fod", urlConfig.getUrl());
        UnirestJsonHeaderConfigurer.configure(unirest);
    }
    
    private static final Map<String, Object> generateTokenRequest(IFoDUserCredentials uc, IFoDUserAuthCode authCode, String... scopes) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("scope", String.join(",", scopes));
        result.put("grant_type", "password");
        result.put("username", String.format("%s\\%s", uc.getTenant(), uc.getUser()));
        result.put("password", String.valueOf(uc.getPassword()));
        if (authCode.getSecurityCode() != null) {
            result.put("security_code", authCode.getSecurityCode());
            result.put("do_totp", authCode.isTotp());
        }
        return result;
    }
    
    private static final Map<String, Object> generateTokenRequest(IFoDClientCredentials cc, String... scopes) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("scope", String.join(",", scopes));
        result.put("grant_type", "client_credentials");
        result.put("client_id", cc.getClientId());
        result.put("client_secret", cc.getClientSecret());
        return result;
    }
}
