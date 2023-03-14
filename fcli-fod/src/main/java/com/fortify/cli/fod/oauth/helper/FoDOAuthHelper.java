package com.fortify.cli.fod.oauth.helper;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.runner.GenericUnirestRunner;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

// TODO Consider moving all classes in this package to a more appropriate package,
//      for example as a sub-package of the 'rest' package.
@Singleton @FixInjection
public class FoDOAuthHelper {
    @Inject private GenericUnirestRunner unirestRunner;
    
    public final FoDTokenCreateResponse createToken(IUrlConfig urlConfig, IFoDUserCredentials uc, String... scopes) {
        Map<String,Object> formData = generateTokenRequest(uc, scopes);
        return unirestRunner.run(unirest->createToken(unirest, urlConfig, formData));
    }

    public final FoDTokenCreateResponse createToken(IUrlConfig urlConfig, IFoDClientCredentials cc, String... scopes) {
        Map<String,Object> formData = generateTokenRequest(cc, scopes);
        return unirestRunner.run(unirest->createToken(unirest, urlConfig, formData));
    }
    
    private FoDTokenCreateResponse createToken(UnirestInstance unirest, IUrlConfig urlConfig, Map<String, Object> formData) {
        configureUnirest(unirest, urlConfig);
        return unirest.post("/oauth/token")
                .accept("application/json")
                .headerReplace("Content-Type", "application/x-www-form-urlencoded")
                .fields(formData)
                .asObject(FoDTokenCreateResponse.class)
                .getBody();
    }
    
    private void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, "fod", urlConfig.getUrl());
        UnirestJsonHeaderConfigurer.configure(unirest);
    }
    
    private Map<String, Object> generateTokenRequest(IFoDUserCredentials uc, String... scopes) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("scope", String.join(",", scopes));
        result.put("grant_type", "password");
        result.put("username", String.format("%s\\%s", uc.getTenant(), uc.getUser()));
        result.put("password", String.valueOf(uc.getPassword()));
        return result;
    }
    
    private Map<String, Object> generateTokenRequest(IFoDClientCredentials cc, String... scopes) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("scope", String.join(",", scopes));
        result.put("grant_type", "client_credentials");
        result.put("client_id", cc.getClientId());
        result.put("client_secret", cc.getClientSecret());
        return result;
    }
}
