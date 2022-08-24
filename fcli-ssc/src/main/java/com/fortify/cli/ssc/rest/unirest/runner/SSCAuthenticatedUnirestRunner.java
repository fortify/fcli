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
package com.fortify.cli.ssc.rest.unirest.runner;

import com.fortify.cli.common.rest.runner.ThrowUnexpectedHttpResponseExceptionInterceptor;
import com.fortify.cli.common.session.unirest.AbstractSessionUnirestRunner;
import com.fortify.cli.ssc.session.manager.SSCSessionData;
import com.fortify.cli.ssc.util.SSCConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

@Singleton @ReflectiveAccess
public class SSCAuthenticatedUnirestRunner extends AbstractSessionUnirestRunner<SSCSessionData> {
	@Override
	protected void configure(String authSessionName, SSCSessionData authSessionData, UnirestInstance unirestInstance) {
		char[] token = authSessionData.getActiveToken();
		if ( token==null ) {
			throw new IllegalStateException("SSC token not available or has expired, please login again");
		}
		setTokenHeader(unirestInstance, token);
		setDefaultHeaders(unirestInstance);
		ThrowUnexpectedHttpResponseExceptionInterceptor.configure(unirestInstance);
	}
	
	private final void setTokenHeader(UnirestInstance unirestInstance, char[] token) {
		final String authHeader = String.format("FortifyToken %s", String.valueOf(token));
		unirestInstance.config().setDefaultHeader("Authorization", authHeader);
	}
	
	private final void setDefaultHeaders(UnirestInstance unirestInstance) {
		unirestInstance.config()
			.setDefaultHeader("Accept", "application/json")
			.setDefaultHeader("Content-Type", "application/json");
	}

	@Override
	public final String getSessionType() {
		return SSCConstants.SESSION_TYPE;
	}

	@Override
	protected Class<SSCSessionData> getSessionDataClass() {
		return SSCSessionData.class;
	}
}
