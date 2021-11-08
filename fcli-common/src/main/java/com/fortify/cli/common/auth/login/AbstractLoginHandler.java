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
package com.fortify.cli.common.auth.login;

import com.fortify.cli.common.auth.logout.LogoutHelper;
import com.fortify.cli.common.auth.session.AuthSessionPersistenceHelper;
import com.fortify.cli.common.auth.session.IAuthSessionData;
import com.fortify.cli.common.auth.session.IAuthSessionTypeProvider;

import jakarta.inject.Inject;
import lombok.Getter;

public abstract class AbstractLoginHandler<C> implements ILoginHandler<C>, IAuthSessionTypeProvider {
	@Getter @Inject private AuthSessionPersistenceHelper authSessionPersistenceHelper;
	@Inject private LogoutHelper logoutHelper;
	
	public final void login(String authSessionName, C loginConfig) {
		logoutIfSessionExists(authSessionName);
		IAuthSessionData authSessionData = _login(authSessionName, loginConfig);
		authSessionPersistenceHelper.saveData(getAuthSessionType(), authSessionName, authSessionData);
	}

	private void logoutIfSessionExists(String authSessionName) {
		String authSessionType = getAuthSessionType();
		if ( authSessionPersistenceHelper.exists(authSessionType, authSessionName) ) {
			// Log out from previous session before creating a new session
			logoutHelper.logoutAndDestroy(authSessionType, authSessionName);
		}
	}

	protected abstract IAuthSessionData _login(String authSessionName, C loginConfig);
}
