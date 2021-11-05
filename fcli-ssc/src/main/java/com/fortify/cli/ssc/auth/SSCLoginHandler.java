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
package com.fortify.cli.ssc.auth;

import com.fortify.cli.common.auth.AbstractLoginHandler;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.rest.data.BasicConnectionConfig;
import com.fortify.cli.common.rest.data.BasicUserCredentialsConfig;
import com.fortify.cli.common.rest.unirest.BasicConnectionConfigUnirestRunner;
import com.fortify.cli.ssc.auth.data.SSCAuthSessionData;
import com.fortify.cli.ssc.auth.data.SSCTokenRequest;
import com.fortify.cli.ssc.auth.data.SSCTokenResponse;
import com.fortify.cli.ssc.rest.data.SSCConnectionConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

@Singleton @ReflectiveAccess
public class SSCLoginHandler extends AbstractLoginHandler<SSCConnectionConfig> {
	@Inject private BasicConnectionConfigUnirestRunner basicUnirestRunner;

	public final String getAuthSessionType() {
		return ProductIdentifiers.SSC;
	}

	@Override
	public final Object _login(String authSessionName, SSCConnectionConfig sscConnectionConfig) {
		SSCAuthSessionData authSessionData = null;
		BasicConnectionConfig basicConnectionConfig = sscConnectionConfig.getBasicConnectionConfig();
		if ( sscConnectionConfig.getToken()!=null ) {
			authSessionData = new SSCAuthSessionData(sscConnectionConfig);
		} else if ( sscConnectionConfig.hasUserCredentialsConfig() ) {
			authSessionData = basicUnirestRunner.runWithUnirest(basicConnectionConfig, unirest->generateAuthSessionData(unirest, sscConnectionConfig));
		} else {
			throw new IllegalArgumentException("Either SSC token or user credentials must be provided");
		}
		return authSessionData;
	}
	
	private final SSCAuthSessionData generateAuthSessionData(UnirestInstance unirest, SSCConnectionConfig sscConnectionConfig) {
		SSCTokenResponse sscTokenResponse = generateToken(unirest, sscConnectionConfig);
		return new SSCAuthSessionData(sscConnectionConfig, sscTokenResponse);
	}
	
	private final SSCTokenResponse generateToken(UnirestInstance unirestInstance, SSCConnectionConfig sscConnectionConfig) {
		SSCTokenRequest tokenRequest = SSCTokenRequest.builder()
				.type("UnifiedLoginToken")
				.terminalDate(sscConnectionConfig.getExpiresAt())
				.build();
		BasicUserCredentialsConfig basicUserCredentialsConfig = sscConnectionConfig.getBasicUserCredentialsConfig();
		return unirestInstance.post("/api/v1/tokens")
				.accept("application/json")
				.header("Content-Type", "application/json")
				.basicAuth(basicUserCredentialsConfig.getUser(), new String(basicUserCredentialsConfig.getPassword()))
				.body(tokenRequest)
				.asObject(SSCTokenResponse.class)
				.ifFailure(resp->{throw new RuntimeException(resp.getStatusText());})
				.getBody();
	}
	
}
