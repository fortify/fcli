/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.rest.unirest;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;

import com.fortify.cli.rest.data.BasicUserCredentialsConfig;
import com.fortify.cli.rest.unirest.AbstractUnirestRunner;
import com.fortify.cli.ssc.rest.data.SSCConnectionConfig;
import com.fortify.cli.ssc.rest.data.SSCLoginSessionData;
import com.fortify.cli.ssc.rest.data.SSCTokenRequest;
import com.fortify.cli.ssc.rest.data.SSCTokenResponse;
import com.fortify.cli.ssc.rest.data.SSCTokenResponse.SSCTokenData;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

@Singleton @ReflectiveAccess
public class SSCUnirestRunner extends AbstractUnirestRunner<SSCLoginSessionData> {
	@Override
	protected void configure(String loginSessionName, SSCLoginSessionData loginSessionData, UnirestInstance unirestInstance) {
		char[] token = null;
		SSCConnectionConfig config = loginSessionData.getConfig();
		// TODO Add null check on config? Shouldn't happen, but still...
		if ( config.getToken()!=null ) {
			token = toBase64Token(loginSessionData.getConfig().getToken());
		} else {
			SSCTokenResponse cachedTokenResponse = loginSessionData.getCachedTokenResponse();
			if ( cachedTokenResponse!=null ) {
				SSCTokenData tokenData = cachedTokenResponse.getData();
				if ( tokenData.getTerminalDate().after(new Date()) ) {
					token = tokenData.getToken();
				} else if ( !config.isRenewAllowed() ) {
					throw new IllegalStateException(String.format("Login session %s for %s has expired, please login again", loginSessionName, getLoginSessionType()));
				}
			} 
		}
		if ( token==null && config.hasUserCredentialsConfig() ) {
			SSCTokenResponse tokenResponse = generateToken(unirestInstance, config);
			loginSessionData.setCachedTokenResponse(tokenResponse);
			if ( !config.isRenewAllowed() ) {
				clearUserCredentials(config.getBasicUserCredentialsConfig());
			}
			getLoginSessionHelper().saveData(getLoginSessionType(), loginSessionName, loginSessionData);
			token = tokenResponse.getData().getToken();
		}
		setTokenHeader(unirestInstance, token);
	}
	
	/**
	 * Make sure that we're using a Base64 encoded token as required by SSC
	 * @param token Encoded or non-encoded token
	 * @return Base64 encoded token
	 */
	private final char[] toBase64Token(char[] token) {
		final byte[] tokenBytes = toByteArray(token);
		final byte[] encodedToken = Base64.isBase64(tokenBytes) ? tokenBytes : Base64.encodeBase64(tokenBytes);
		return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encodedToken)).array();
	}
	
	private final void setTokenHeader(UnirestInstance unirestInstance, char[] token) {
		final String authHeader = String.format("FortifyToken %s", String.valueOf(token));
		unirestInstance.config().setDefaultHeader("Authorization", authHeader);
	}
	
	private final void clearUserCredentials(BasicUserCredentialsConfig config) {
		Arrays.fill(config.getPassword(), 'x');
		config.setPassword(null);
		config.setUser(null);
	}  
	
	private final byte[] toByteArray(char[] input) {
		ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(input));
		byte[] result = new byte[bb.remaining()];
		bb.get(result);
		return result;
	}

	private final SSCTokenResponse generateToken(UnirestInstance unirestInstance, SSCConnectionConfig config) {
		SSCTokenRequest tokenRequest = SSCTokenRequest.builder().type("UnifiedLoginToken").build();
		if ( !config.hasUserCredentialsConfig() ) {
			throw new IllegalStateException("No login credentials available");
		}
		BasicUserCredentialsConfig ucConfig = config.getBasicUserCredentialsConfig();
		return unirestInstance.post("/api/v1/tokens")
				.accept("application/json")
				.header("Content-Type", "application/json")
				.basicAuth(ucConfig.getUser(), new String(ucConfig.getPassword()))
				.body(tokenRequest)
				.asObject(SSCTokenResponse.class).getBody();
	}
	
	/*
	@Override
	protected Object login() {
		SSCConnectionConfig config = getRestConnectionConfig();
		SSCTokenResponse tokenResponse = null;
		if ( config.getAuthType()==SSCAuthType.USER ) {
			tokenResponse = authenticateWithUserCredentials(config);
			if ( !config.isAllowRenew() ) {
				clearUserCredentials(config);
			}
		}
		return new SSCLoginSessionData(config, tokenResponse);
	}
	
	@Override
	public void logout(SSCLoginSessionData sessionData) {
		var cachedTokenResponse = sessionData.getCachedTokenResponse();
		var tokenData = cachedTokenResponse==null ? null : cachedTokenResponse.getData();
		if ( tokenData!=null ) {
			logout(tokenData);
		}
	}

	private void logout(SSCTokenData tokenData) {
		if ( tokenData.getTerminalDate().after(new Date())) {
			System.out.println("Logging out from SSC");
		}
	}
		
	*/

	@Override
	public final String getLoginSessionType() {
		return "ssc";
	}

	@Override
	protected Class<SSCLoginSessionData> getLoginSessionDataClass() {
		return SSCLoginSessionData.class;
	}
}
