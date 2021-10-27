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
package com.fortify.cli.common.auth;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;

@Singleton
public final class LogoutHelper {
	private final AuthSessionPersistenceHelper authSessionPersistenceHelper;
	@Getter private Map<String, ILogoutHandler> logoutHandlers;
	
	@Inject
	public LogoutHelper(AuthSessionPersistenceHelper authSessionPersistenceHelper) {
		this.authSessionPersistenceHelper = authSessionPersistenceHelper;
	}
	
	@Inject
	public void setLogoutManagers(Collection<ILogoutHandler> logoutHandlers) {
		this.logoutHandlers = logoutHandlers.stream().collect(
			Collectors.toMap(ILogoutHandler::getAuthSessionType, Function.identity()));
	}
	
	public final void logoutAndDestroy(String authSessionType, String authSessionName) {
		logoutHandlers.get(authSessionType).logout(authSessionName);
		authSessionPersistenceHelper.destroy(authSessionType, authSessionName);
	}
}
