package com.fortify.cli.ssc.token.helper;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.runner.UnirestRunner;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentialsConfig;
import com.fortify.cli.common.rest.runner.config.UnirestBasicAuthConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.ssc.rest.SSCUrls;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

@Singleton @FixInjection
public class SSCTokenHelper {
    @Inject private UnirestRunner unirestRunner;
    
    public final JsonNode listTokens(IUrlConfig urlConfig, IUserCredentialsConfig uc, Map<String,Object> queryParams) {
        return unirestRunner.run(unirest->listTokens(unirest, urlConfig, uc, queryParams));
    }
    
    public final JsonNode updateToken(IUrlConfig urlConfig, IUserCredentialsConfig uc, String tokenId, SSCTokenUpdateRequest tokenUpdateRequest) {
        return unirestRunner.run(unirest->updateToken(unirest, urlConfig, uc, tokenId, tokenUpdateRequest));
    }
    
    public final JsonNode deleteTokensById(IUrlConfig urlConfig, IUserCredentialsConfig uc, String... tokenIds) {
        return unirestRunner.run(unirest->deleteTokensById(unirest, urlConfig, uc, tokenIds));
    }
    
    public final JsonNode deleteTokensByValue(IUrlConfig urlConfig, IUserCredentialsConfig uc, String... tokens) {
        return unirestRunner.run(unirest->deleteTokensByValue(unirest, urlConfig, uc, tokens));
    }
    
    public final <T> T createToken(IUrlConfig urlConfig, IUserCredentialsConfig uc, SSCTokenCreateRequest tokenCreateRequest, Class<T> returnType) {
        return unirestRunner.run(unirest->createToken(unirest, urlConfig, uc, tokenCreateRequest, returnType));
    }
    
    public final <R> R run(IUrlConfig urlConfig, char[] activeToken, Function<UnirestInstance, R> f) {
        return unirestRunner.run(unirest->{
            configureUnirest(unirest, urlConfig, activeToken);
            return f.apply(unirest);
        });
    }

    private final JsonNode listTokens(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, Map<String, Object> queryParams) {
        configureUnirest(unirest, urlConfig, uc);
        GetRequest request = unirest.get(SSCUrls.TOKENS);
        if ( queryParams!=null ) {
            request = request.queryString(queryParams);
        }
        return request.asObject(JsonNode.class).getBody();
    }
    
    private final JsonNode updateToken(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, String tokenId, SSCTokenUpdateRequest tokenUpdateRequest) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.put(SSCUrls.TOKEN(tokenId))
                .body(tokenUpdateRequest)
                .asObject(JsonNode.class).getBody();
    }
    
    private final JsonNode deleteTokensById(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, String... tokenIds) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.delete(SSCUrls.TOKENS)
                .queryString("ids", String.join(",", tokenIds))
                .asObject(JsonNode.class).getBody();
    }
    
    private final JsonNode deleteTokensByValue(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, String... tokens) {
        configureUnirest(unirest, urlConfig, uc);
        ObjectNode tokenRevokeRequest = new ObjectMapper().createObjectNode();
        ArrayNode tokenArray = Stream.of(tokens).map(SSCTokenConverter::toRestToken).map(TextNode::new).collect(JsonHelper.arrayNodeCollector());
        tokenRevokeRequest.set("tokens", tokenArray);
        return unirest.post(SSCUrls.TOKENS_ACTION_REVOKE)
                .body(tokenRevokeRequest)
                .asObject(JsonNode.class).getBody();
    }
    
    private final <T> T createToken(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, SSCTokenCreateRequest tokenCreateRequest, Class<T> returnType) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.post("/api/v1/tokens")
                .body(tokenCreateRequest)
                .asObject(returnType)
                .getBody();
    }
    
    private void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        UnirestBasicAuthConfigurer.configure(unirest, uc);
        UnirestJsonHeaderConfigurer.configure(unirest);
    }
    
    private static final void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig, char[] activeToken) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        unirest.config().requestCompression(false); // TODO For some reason, in native binaries, compression may cause issues
        unirest.config().addDefaultHeader("Authorization", "FortifyToken "+new String(activeToken));
    }
}
