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
package com.fortify.cli.sc_sast.rest.runner;

import com.fortify.cli.common.rest.runner.ThrowUnexpectedHttpResponseExceptionInterceptor;
import com.fortify.cli.common.session.rest.runner.AbstractSessionUnirestRunner;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;
import com.fortify.cli.sc_sast.util.SCSastConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

@Singleton @ReflectiveAccess
public class SCSastAuthenticatedUnirestRunner extends AbstractSessionUnirestRunner<SCSastSessionData> {
	@Override
	protected void configure(String authSessionName, SCSastSessionData authSessionData, UnirestInstance unirestInstance) {
		char[] token = authSessionData.getClientAuthToken();
		if ( token==null ) {
			throw new IllegalStateException("ScanCentral SAST client auth token not available, please login");
		}
		setTokenHeader(unirestInstance, token);
		ThrowUnexpectedHttpResponseExceptionInterceptor.configure(unirestInstance);
	}
	
	private final void setTokenHeader(UnirestInstance unirestInstance, char[] token) {
		unirestInstance.config()
			.setDefaultHeader("fortify-client", String.valueOf(token))
			.setDefaultHeader("Accept", "application/json");
	}

	@Override
	public final String getSessionType() {
		return SCSastConstants.SESSION_TYPE;
	}

	@Override
	protected Class<SCSastSessionData> getSessionDataClass() {
		return SCSastSessionData.class;
	}
}
