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
package com.fortify.cli.sc_sast.session.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.unirest.exception.ThrowUnexpectedHttpResponseExceptionInterceptor;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.spi.AbstractSessionHandlerAction;
import com.fortify.cli.sc_sast.rest.unirest.runner.SCSastAuthenticatedUnirestRunner;
import com.fortify.cli.sc_sast.util.SCSastConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@Singleton @ReflectiveAccess
public class SCSastSessionLoginHandler extends AbstractSessionHandlerAction<SCSastSessionLoginConfig> {
	@Getter @Inject private SCSastAuthenticatedUnirestRunner unirestRunner;
	
	public final String getSessionType() {
		return SCSastConstants.SESSION_TYPE;
	}

	@Override
	public final ISessionData _login(String authSessionName, SCSastSessionLoginConfig sscLoginConfig) {
		SCSastSessionData sessionData = null;
		if ( sscLoginConfig.getClientAuthToken()!=null ) {
			sessionData = new SCSastSessionData(sscLoginConfig);
		} else {
			throw new IllegalArgumentException("ScanCentral SAST client auth token must be provided ");
		}
		return sessionData;
	}
	
	@Override
	protected void testAuthenticatedConnection(String authSessionName, SCSastSessionLoginConfig loginConfig) {
		unirestRunner.runWithUnirest(authSessionName, this::testWithUnirest);
	}
	
	protected Void testWithUnirest(UnirestInstance unirest) {
		// TODO Review this
		ThrowUnexpectedHttpResponseExceptionInterceptor.configure(unirest);
		unirest.get("/rest/v2/ping").asObject(JsonNode.class).getBody();
		return null;
	}
}
