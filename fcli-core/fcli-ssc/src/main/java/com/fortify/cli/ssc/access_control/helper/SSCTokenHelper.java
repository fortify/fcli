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
package com.fortify.cli.ssc.access_control.helper;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestBasicAuthConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Help.Ansi;

public class SSCTokenHelper {
    public static final JsonNode transformTokenRecord(JsonNode tokenRecord) {
    	if ( tokenRecord instanceof ObjectNode && tokenRecord.has("terminalDate") ) {
    		Date terminalDate = JsonHelper.treeToValue(tokenRecord.get("terminalDate"), Date.class);
    		Duration diff = Duration.between(new Date().toInstant(), terminalDate.toInstant());
    		String timeRemaining;
    		if ( diff.toDays()>0 ) {
    			timeRemaining = String.format("%d days", diff.toDays()+1);
    		} else if ( diff.toHours()>0 ) {
    			timeRemaining = String.format("%d hours", diff.toHours()+1);
    		} else if (diff.toMinutes()>0 ) {
    			timeRemaining = String.format("%d minutes", diff.toMinutes()+1);
    		} else {
    			timeRemaining = Ansi.AUTO.string("0");
    		}
    		((ObjectNode)tokenRecord).put("timeRemaining", timeRemaining);
    	}
    	return tokenRecord;
    }
    
    public static final JsonNode listTokens(IUrlConfig urlConfig, IUserCredentialsConfig uc, Map<String,Object> queryParams) {
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            return listTokens(unirest, urlConfig, uc, queryParams);
        }
    }
    
    public static final JsonNode updateToken(IUrlConfig urlConfig, IUserCredentialsConfig uc, String tokenId, SSCTokenUpdateRequest tokenUpdateRequest) {
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            return updateToken(unirest, urlConfig, uc, tokenId, tokenUpdateRequest);
        }
    }
    
    public static final JsonNode deleteTokensById(IUrlConfig urlConfig, IUserCredentialsConfig uc, String... tokenIds) {
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            return deleteTokensById(unirest, urlConfig, uc, tokenIds);
        }
    }
    
    public static final JsonNode deleteTokensByValue(IUrlConfig urlConfig, IUserCredentialsConfig uc, String... tokens) {
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            return deleteTokensByValue(unirest, urlConfig, uc, tokens);
        }
    }
    
    public static final void revokeToken(IUrlConfig urlConfig, IUserCredentialsConfig uc, char[] token) {
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            revokeToken(unirest, urlConfig, uc, token);
        }
    }
    
    public static final <T> T createToken(IUrlConfig urlConfig, IUserCredentialsConfig uc, SSCTokenCreateRequest tokenCreateRequest, Class<T> returnType) {
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            return createToken(unirest, urlConfig, uc, tokenCreateRequest, returnType);
        }
    }
    
    public static final SSCTokenGetOrCreateResponse getTokenData(IUrlConfig urlConfig, char[] token) {
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            return getTokenData(unirest, urlConfig, token);
        }
    }

    public static final <R> R run(IUrlConfig urlConfig, char[] activeToken, Function<UnirestInstance, R> f) {
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            configureUnirest(unirest, urlConfig, activeToken);
            return f.apply(unirest);
        }
    }

    private static final JsonNode listTokens(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, Map<String, Object> queryParams) {
        configureUnirest(unirest, urlConfig, uc);
        GetRequest request = unirest.get(SSCUrls.TOKENS);
        if ( queryParams!=null ) {
            request = request.queryString(queryParams);
        }
        return request.asObject(JsonNode.class).getBody();
    }
    
    private static final JsonNode updateToken(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, String tokenId, SSCTokenUpdateRequest tokenUpdateRequest) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.put(SSCUrls.TOKEN(tokenId))
                .body(tokenUpdateRequest)
                .asObject(JsonNode.class).getBody();
    }
    
    private static final JsonNode deleteTokensById(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, String... tokenIds) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.delete(SSCUrls.TOKENS)
                .queryString("ids", String.join(",", tokenIds))
                .asObject(JsonNode.class).getBody();
    }
    
    private static final JsonNode deleteTokensByValue(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, String... tokens) {
        configureUnirest(unirest, urlConfig, uc);
        ObjectNode tokenRevokeRequest = new ObjectMapper().createObjectNode();
        ArrayNode tokenArray = Stream.of(tokens).map(SSCTokenConverter::toRestToken).map(TextNode::new).collect(JsonHelper.arrayNodeCollector());
        tokenRevokeRequest.set("tokens", tokenArray);
        return unirest.post(SSCUrls.TOKENS_ACTION_REVOKE)
                .body(tokenRevokeRequest)
                .asObject(JsonNode.class).getBody();
    }
    
    private static final void revokeToken(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, char[] restToken) {
        if ( uc!=null ) {
            configureUnirest(unirest, urlConfig, uc);
        } else {
            // SSC 24.2+ allows for revoking tokens using token authentication
            configureUnirest(unirest, urlConfig, restToken);
        }
        ObjectNode tokenRevokeRequest = new ObjectMapper().createObjectNode();
        ArrayNode tokenArray = JsonHelper.toArrayNode(new String(restToken));
        tokenRevokeRequest.set("tokens", tokenArray);
        unirest.post(SSCUrls.TOKENS_ACTION_REVOKE)
                .body(tokenRevokeRequest)
                .asObject(JsonNode.class).getBody();
    }
    
    private static final <T> T createToken(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc, SSCTokenCreateRequest tokenCreateRequest, Class<T> returnType) {
        configureUnirest(unirest, urlConfig, uc);
        return unirest.post("/api/v1/tokens")
                .body(tokenCreateRequest)
                .asObject(returnType)
                .getBody();
    }
    
    private static SSCTokenGetOrCreateResponse getTokenData(UnirestInstance unirest, IUrlConfig urlConfig, char[] token) {
        configureUnirest(unirest, urlConfig, token);
        try {
            var result = unirest.post("/api/v1/userSession/tokenData")
                    .body(JsonHelper.getObjectMapper().createObjectNode())
                    .asObject(SSCTokenGetOrCreateResponse.class)
                    .getBody();
            result.getData().setToken(token);
            return result;
        } catch ( UnexpectedHttpResponseException e ) {
            if ( e.getStatus()==404 || e.getStatus()==401 ) {
                // Older SSC versions don't support this endpoint, so we just return null
                return null; 
            } else {
                throw e;
            }
        }
    }
    
    private static void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig, IUserCredentialsConfig uc) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, "ssc", urlConfig.getUrl());
        UnirestBasicAuthConfigurer.configure(unirest, uc);
        UnirestJsonHeaderConfigurer.configure(unirest);
    }
    
    public static final void configureUnirest(UnirestInstance unirest, IUrlConfig urlConfig, char[] activeToken) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, "ssc", urlConfig.getUrl());
        unirest.config().requestCompression(false); // TODO For some reason, in native binaries, compression may cause issues
        unirest.config().setDefaultHeader("Authorization", "FortifyToken "+new String(activeToken));
    }
}
