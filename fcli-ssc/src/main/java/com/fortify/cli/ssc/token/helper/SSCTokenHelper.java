package com.fortify.cli.ssc.token.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentials;
import com.fortify.cli.common.rest.runner.config.UnirestBasicAuthConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.ssc.rest.SSCUrls;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

@Singleton @FixInjection
public class SSCTokenHelper {
    @Inject private UnirestRunner unirestRunner;
    
    public final JsonNode deleteTokensById(IUrlConfig urlConfig, IUserCredentials uc, String... tokenIds) {
        return unirestRunner.run(unirest->deleteTokensById(unirest, urlConfig, uc, tokenIds));
    }
    
    public final SSCTokenCreateResponse createToken(IUrlConfig urlConfig, IUserCredentials uc, SSCTokenCreateRequest tokenCreateRequest) {
        return unirestRunner.run(unirest->createToken(unirest, urlConfig, uc, tokenCreateRequest));
    }
    
    private final JsonNode deleteTokensById(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc, String... tokenIds) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.delete(SSCUrls.TOKENS)
                .queryString("ids", String.join(",", tokenIds))
                .asObject(JsonNode.class).getBody();
    }
    
    private final SSCTokenCreateResponse createToken(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc, SSCTokenCreateRequest tokenCreateRequest) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.post("/api/v1/tokens")
                .body(tokenCreateRequest)
                .asObject(SSCTokenCreateResponse.class)
                .getBody();
    }
    
    private void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        UnirestBasicAuthConfigurer.configure(unirest, uc);
        UnirestJsonHeaderConfigurer.configure(unirest);
    }
}
