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
package com.fortify.cli.fod.auth.login;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fortify.cli.common.auth.login.AbstractLoginHandler;
import com.fortify.cli.common.auth.session.IAuthSessionData;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.rest.data.IConnectionConfig;
import com.fortify.cli.common.rest.unirest.ConnectionConfigUnirestRunner;
import com.fortify.cli.common.rest.unirest.IfFailure;
import com.fortify.cli.fod.auth.login.rest.FoDTokenResponse;
import com.fortify.cli.fod.auth.session.FoDAuthSessionData;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

@Singleton @ReflectiveAccess
public class FoDLoginHandler extends AbstractLoginHandler<FoDLoginConfig> {
	@Inject private ConnectionConfigUnirestRunner basicUnirestRunner;

	public final String getAuthSessionType() {
		return ProductIdentifiers.FOD;
	}

	@Override
	public final IAuthSessionData _login(String authSessionName, FoDLoginConfig fodLoginConfig) {
		FoDAuthSessionData authSessionData = null;
		IConnectionConfig connectionConfig = fodLoginConfig.getConnectionConfig();
		if ( fodLoginConfig.hasClientCredentials() ) {
			authSessionData = basicUnirestRunner.runWithUnirest(connectionConfig, unirest->generateClientCredentialsAuthSessionData(unirest, fodLoginConfig));
		} else if ( fodLoginConfig.hasUserCredentialsConfig() ) {
			authSessionData = basicUnirestRunner.runWithUnirest(connectionConfig, unirest->generateUserCredentialsAuthSessionData(unirest, fodLoginConfig));
		} else {
			throw new IllegalArgumentException("Either SSC token or user credentials must be provided");
		}
		return authSessionData;
	}
	
	private final FoDAuthSessionData generateClientCredentialsAuthSessionData(UnirestInstance unirest, FoDLoginConfig fodLoginConfig) {
		FoDTokenResponse tokenResponse = generateToken(unirest, getClientCredentialsTokenRequest(fodLoginConfig));
		return new FoDAuthSessionData(fodLoginConfig, tokenResponse);
	}
	
	private final FoDAuthSessionData generateUserCredentialsAuthSessionData(UnirestInstance unirest, FoDLoginConfig fodLoginConfig) {
		FoDTokenResponse tokenResponse = generateToken(unirest, getUserCredentialsTokenRequest(fodLoginConfig));
		return new FoDAuthSessionData(fodLoginConfig, tokenResponse);
	}
	
	private Map<String, Object> getUserCredentialsTokenRequest(FoDLoginConfig config) {
		Map<String,Object> result = new LinkedHashMap<>();
		result.put("scope", String.join(",", config.getScopes()));
		result.put("grant_type", "password");
		result.put("username", String.format("%s\\%s", config.getFodUserCredentialsConfig().getTenant(), config.getFodUserCredentialsConfig().getUser()));
		result.put("password", String.valueOf(config.getFodUserCredentialsConfig().getPassword()));
		return result;
	}
	
	private Map<String, Object> getClientCredentialsTokenRequest(FoDLoginConfig config) {
		Map<String,Object> result = new LinkedHashMap<>();
		result.put("scope", String.join(",", config.getScopes()));
		result.put("grant_type", "client_credentials");
		result.put("client_id", config.getFodClientCredentialsConfig().getClientId());
		result.put("client_secret", config.getFodClientCredentialsConfig().getClientSecret());
		return result;
	}
	
	private final FoDTokenResponse generateToken(UnirestInstance unirestInstance, Map<String, Object> tokenRequestData) {
		 return unirestInstance.post("/oauth/token")
				.accept("application/json")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.fields(tokenRequestData)
				.asObject(FoDTokenResponse.class)
				.ifFailure(IfFailure::handle)
				.getBody();
	}

}
