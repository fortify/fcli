package com.fortify.cli.ssc.token.helper;

import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentials;
import com.fortify.cli.common.rest.runner.config.UnirestBasicAuthConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.common.util.JsonHelper;
import com.fortify.cli.ssc.rest.SSCUrls;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

@Singleton @FixInjection
public class SSCTokenHelper {
    @Inject private UnirestRunner unirestRunner;
    
    public final JsonNode listTokens(IUrlConfig urlConfig, IUserCredentials uc, Map<String,Object> queryParams) {
        return unirestRunner.run(unirest->listTokens(unirest, urlConfig, uc, queryParams));
    }
    
    public final JsonNode updateToken(IUrlConfig urlConfig, IUserCredentials uc, String tokenId, SSCTokenUpdateRequest tokenUpdateRequest) {
        return unirestRunner.run(unirest->updateToken(unirest, urlConfig, uc, tokenId, tokenUpdateRequest));
    }
    
    public final JsonNode deleteTokensById(IUrlConfig urlConfig, IUserCredentials uc, String... tokenIds) {
        return unirestRunner.run(unirest->deleteTokensById(unirest, urlConfig, uc, tokenIds));
    }
    
    public final JsonNode deleteTokensByValue(IUrlConfig urlConfig, IUserCredentials uc, String... tokens) {
        return unirestRunner.run(unirest->deleteTokensByValue(unirest, urlConfig, uc, tokens));
    }
    
    public final <T> T createToken(IUrlConfig urlConfig, IUserCredentials uc, SSCTokenCreateRequest tokenCreateRequest, Class<T> returnType) {
        return unirestRunner.run(unirest->createToken(unirest, urlConfig, uc, tokenCreateRequest, returnType));
    }
    
    private final JsonNode listTokens(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc, Map<String, Object> queryParams) {
        configureUnirest(unirest, urlConfig, uc);
        GetRequest request = unirest.get(SSCUrls.TOKENS);
        if ( queryParams!=null ) {
            request = request.queryString(queryParams);
        }
        return request.asObject(JsonNode.class).getBody();
    }
    
    private final JsonNode updateToken(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc, String tokenId, SSCTokenUpdateRequest tokenUpdateRequest) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.put(SSCUrls.TOKEN(tokenId))
                .body(tokenUpdateRequest)
                .asObject(JsonNode.class).getBody();
    }
    
    private final JsonNode deleteTokensById(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc, String... tokenIds) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.delete(SSCUrls.TOKENS)
                .queryString("ids", String.join(",", tokenIds))
                .asObject(JsonNode.class).getBody();
    }
    
    private final JsonNode deleteTokensByValue(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc, String... tokens) {
        configureUnirest(unirest, urlConfig, uc);
        ObjectNode tokenRevokeRequest = new ObjectMapper().createObjectNode();
        ArrayNode tokenArray = Stream.of(tokens).map(SSCTokenConverter::toRestToken).map(TextNode::new).collect(JsonHelper.arrayNodeCollector());
        tokenRevokeRequest.set("tokens", tokenArray);
        return unirest.post(SSCUrls.TOKENS_ACTION_REVOKE)
                .body(tokenRevokeRequest)
                .asObject(JsonNode.class).getBody();
    }
    
    private final <T> T createToken(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc, SSCTokenCreateRequest tokenCreateRequest, Class<T> returnType) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.post("/api/v1/tokens")
                .body(tokenCreateRequest)
                .asObject(returnType)
                .getBody();
    }
    
    private void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentials uc) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        UnirestBasicAuthConfigurer.configure(unirest, uc);
        UnirestJsonHeaderConfigurer.configure(unirest);
    }
}
