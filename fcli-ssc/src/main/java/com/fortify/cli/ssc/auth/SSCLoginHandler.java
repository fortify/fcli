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

import java.util.Date;

import com.fortify.cli.common.auth.AuthSessionPersistenceHelper;
import com.fortify.cli.common.auth.ILoginHandler;
import com.fortify.cli.common.auth.LogoutHelper;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.ssc.auth.data.SSCAuthSessionData;
import com.fortify.cli.ssc.rest.data.SSCConnectionConfig;
import com.fortify.cli.ssc.rest.unirest.SSCUnirestRunner;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@Singleton @ReflectiveAccess
public class SSCLoginHandler implements ILoginHandler<SSCConnectionConfig> {
	@Getter @Inject private SSCUnirestRunner unirestRunner;
	@Getter @Inject private AuthSessionPersistenceHelper authSessionPersistenceHelper;
	@Getter @Inject private LogoutHelper logoutHelper;

	public final String getAuthSessionType() {
		return ProductIdentifiers.SSC;
	}

	@Override
	public void login(String authSessionName, SSCConnectionConfig connectionConfig) {
		String authSessionType = getAuthSessionType();
		if ( authSessionPersistenceHelper.exists(authSessionType, authSessionName) ) {
			// Log out from previous session before creating a new session
			getLogoutHelper().logoutAndDestroy(authSessionType, authSessionName);
		}
		SSCAuthSessionData authSessionData = new SSCAuthSessionData(connectionConfig, null, new Date());
		getUnirestRunner().runWithUnirest(authSessionName, authSessionData, this::validateAuthSession);
		authSessionPersistenceHelper.saveData(authSessionType, authSessionName, authSessionData);
	}
	
	private final Void validateAuthSession(UnirestInstance unirestInstance) {
		// TODO Is there any endpoint that can be called with any type of token?
		return null;
	}
}
