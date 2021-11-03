/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.fod.rest.unirest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.rest.unirest.AbstractAuthSessionUnirestRunner;
import com.fortify.cli.fod.auth.data.FoDAuthSessionData;
import com.fortify.cli.fod.auth.data.FoDTokenResponse;
import com.fortify.cli.fod.rest.data.FoDClientCredentialsConfig;
import com.fortify.cli.fod.rest.data.FoDConnectionConfig;
import com.fortify.cli.fod.rest.data.FoDUserCredentialsConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

@Singleton @ReflectiveAccess
public class FoDUnirestRunner extends AbstractAuthSessionUnirestRunner<FoDAuthSessionData> {
	@Override
	protected void configure(String authSessionName, FoDAuthSessionData authSessionData, UnirestInstance unirestInstance) {
		FoDConnectionConfig config = authSessionData.getConfig();
		if ( config==null ) {
			throw new IllegalStateException("FoD connection configuration may not be null");
		}
		setBearerHeader(unirestInstance, getFoDBearerToken(authSessionName, authSessionData, unirestInstance, config));
	}

	private String getFoDBearerToken(String authSessionName, FoDAuthSessionData authSessionData, UnirestInstance unirestInstance, FoDConnectionConfig config) {
		String token = null;
		FoDTokenResponse cachedTokenResponse = authSessionData.getCachedTokenResponse();
		if ( cachedTokenResponse!=null ) {
			if ( !cachedTokenResponse.isExpired() ) {
				token = cachedTokenResponse.getAccess_token();
			} else if ( !config.isRenewAllowed() ) {
				throw new IllegalStateException(String.format("Login session %s for %s has expired, please login again", authSessionName, getAuthSessionType()));
			}
		} 
		if ( token==null ) {
			Map<String, Object> tokenRequestData = null;
			if ( config.hasUserCredentialsConfig() ) {
				tokenRequestData = getUserCredentialsTokenRequest(config);
			} else if ( config.hasClientCredentials() ) {
				tokenRequestData = getClientCredentialsTokenRequest(config);
			}
			if ( tokenRequestData != null ) {
				FoDTokenResponse tokenResponse = generateToken(unirestInstance, tokenRequestData);
				authSessionData.setCachedTokenResponse(tokenResponse);
				if ( !config.isRenewAllowed() ) {
					clearUserCredentials(config.getFodUserCredentialsConfig());
					clearClientCredentials(config.getFodClientCredentialsConfig());
				}
				getAuthSessionPersistenceHelper().saveData(getAuthSessionType(), authSessionName, authSessionData);
				token = tokenResponse.getAccess_token();
			} else {
				throw new IllegalStateException("Cannot generate bearer token");
			}
		}
		return token;
	}
	
	private Map<String, Object> getUserCredentialsTokenRequest(FoDConnectionConfig config) {
		Map<String,Object> result = new LinkedHashMap<>();
		result.put("scope", String.join(",", config.getScopes()));
		result.put("grant_type", "password");
		result.put("username", String.format("%s\\%s", config.getNonNullUserCredentialsConfig().getTenant(), config.getNonNullUserCredentialsConfig().getUser()));
		result.put("password", String.valueOf(config.getNonNullUserCredentialsConfig().getPassword()));
		return result;
	}
	
	private Map<String, Object> getClientCredentialsTokenRequest(FoDConnectionConfig config) {
		Map<String,Object> result = new LinkedHashMap<>();
		result.put("scope", String.join(",", config.getScopes()));
		result.put("grant_type", "client_credentials");
		result.put("client_id", config.getNonNullClientCredentialsConfig().getClientId());
		result.put("client_secret", config.getNonNullClientCredentialsConfig().getClientSecret());
		return result;
	}
	
	private final void setBearerHeader(UnirestInstance unirestInstance, String token) {
		final String authHeader = String.format("Bearer %s", token);
		unirestInstance.config().setDefaultHeader("Authorization", authHeader);
	}
	
	private final void clearUserCredentials(FoDUserCredentialsConfig config) {
		Arrays.fill(config.getPassword(), 'x');
		config.setPassword(null);
		config.setUser(null);
	}  
	
	private final void clearClientCredentials(FoDClientCredentialsConfig config) {
		config.setClientId(null);
		config.setClientSecret(null);
	}

	private final FoDTokenResponse generateToken(UnirestInstance unirestInstance, Map<String, Object> tokenRequestData) {
		 return unirestInstance.post("/oauth/token")
				.accept("application/json")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.fields(tokenRequestData)
				.asObject(FoDTokenResponse.class).getBody();
	}

	@Override
	public final String getAuthSessionType() {
		return ProductIdentifiers.FOD;
	}

	@Override
	protected Class<FoDAuthSessionData> getAuthSessionDataClass() {
		return FoDAuthSessionData.class;
	}
}
