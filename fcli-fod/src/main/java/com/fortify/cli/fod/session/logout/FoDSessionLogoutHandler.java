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
package com.fortify.cli.fod.session.logout;

import com.fortify.cli.common.session.SessionPersistenceHelper;
import com.fortify.cli.common.session.logout.ISessionLogoutHandler;
import com.fortify.cli.fod.FoDConstants;
import com.fortify.cli.fod.rest.unirest.runner.FoDAuthenticatedUnirestRunner;
import com.fortify.cli.fod.session.FoDSessionData;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@Singleton @ReflectiveAccess
public class FoDSessionLogoutHandler implements ISessionLogoutHandler {
	@Getter @Inject private SessionPersistenceHelper sessionPersistenceHelper;
	@Getter @Inject private FoDAuthenticatedUnirestRunner unirestRunner;

	@Override
	public final void logout(String authSessionName) {
		FoDSessionData data = sessionPersistenceHelper.getData(getSessionType(), authSessionName, FoDSessionData.class);
		if ( data.hasActiveCachedTokenResponse() ) {
			unirestRunner.runWithUnirest(authSessionName, unirestInstance->logout(unirestInstance, data));
		}
	}
	
	private final Void logout(UnirestInstance unirestInstance, FoDSessionData authSessionData) {
		try {
			// TODO Invalidate token if possible in FoD
		} catch ( RuntimeException e ) {
			System.out.println("Error deserializing token:" + e.getMessage());
		}
		return null;
	}

	@Override
	public String getSessionType() {
		return FoDConstants.SESSION_TYPE;
	}
}
