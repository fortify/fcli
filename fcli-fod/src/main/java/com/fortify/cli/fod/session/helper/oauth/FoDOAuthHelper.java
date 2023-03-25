package com.fortify.cli.fod.session.helper.oauth;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;

import kong.unirest.UnirestInstance;

// TODO Consider moving all classes in this package to a more appropriate package,
//      for example as a sub-package of the 'rest' package.
public class FoDOAuthHelper {
    public static final FoDTokenCreateResponse createToken(IUrlConfig urlConfig, IFoDUserCredentials uc, String... scopes) {
        Map<String,Object> formData = generateTokenRequest(uc, scopes);
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            return createToken(unirest, urlConfig, formData);
        }
    }

    public static final FoDTokenCreateResponse createToken(IUrlConfig urlConfig, IFoDClientCredentials cc, String... scopes) {
        Map<String,Object> formData = generateTokenRequest(cc, scopes);
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            return createToken(unirest, urlConfig, formData);
        }
    }
    
    private static final FoDTokenCreateResponse createToken(UnirestInstance unirest, IUrlConfig urlConfig, Map<String, Object> formData) {
        configureUnirest(unirest, urlConfig);
        return unirest.post("/oauth/token")
                .accept("application/json")
                .headerReplace("Content-Type", "application/x-www-form-urlencoded")
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
    
    private static final Map<String, Object> generateTokenRequest(IFoDUserCredentials uc, String... scopes) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("scope", String.join(",", scopes));
        result.put("grant_type", "password");
        result.put("username", String.format("%s\\%s", uc.getTenant(), uc.getUser()));
        result.put("password", String.valueOf(uc.getPassword()));
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
