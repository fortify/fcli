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
package com.fortify.cli.fod.auth.data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fortify.cli.common.auth.AuthSessionSummary;
import com.fortify.cli.common.rest.data.BasicConnectionConfig;
import com.fortify.cli.common.rest.data.IBasicConnectionConfigProvider;
import com.fortify.cli.fod.rest.data.FoDConnectionConfig;

import io.micronaut.core.annotation.Introspected;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Introspected @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class FoDAuthSessionData implements IBasicConnectionConfigProvider {
	private FoDConnectionConfig config;
	private FoDTokenResponse cachedTokenResponse;
	private Date created; // When was this session created; note that due to token renewal we cannot use creation data from cache token response
	
	@Override @JsonIgnore
	public BasicConnectionConfig getBasicConnectionConfig() {
		return config.getNonNullBasicConnectionConfig();
	}
	
	@JsonIgnore
	public final boolean hasActiveCachedTokenResponse() {
		return getCachedTokenResponse()!=null && cachedTokenResponse.isExpired(); 
	}

	@JsonIgnore
	public AuthSessionSummary getSummary(String authSessionName) {
		return AuthSessionSummary.builder()
				.name(authSessionName)
				.created(getCreated())
				.expires(getSessionExpiryDate())
				.build();
	}
	
	@JsonIgnore
	private Date getSessionExpiryDate() {
		Date sessionExpiryDate = AuthSessionSummary.EXPIRES_UNKNOWN;
		if ( getCachedTokenResponse()!=null ) {
			sessionExpiryDate = new Date(getCachedTokenResponse().getExpiresAt());
		}
		return sessionExpiryDate;
	}
}
