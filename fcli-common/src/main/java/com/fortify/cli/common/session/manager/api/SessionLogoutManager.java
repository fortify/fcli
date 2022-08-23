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
package com.fortify.cli.common.session.manager.api;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fortify.cli.common.session.manager.spi.ISessionLogoutHandler;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class SessionLogoutManager {
	private final SessionDataManager sessionDataManager;
	private final Map<String, ISessionLogoutHandler> sessionLogoutHandlers;
	
	@Inject
	SessionLogoutManager(SessionDataManager sessionDataManager, Collection<ISessionLogoutHandler> sessionLogoutHandlers) {
		this.sessionDataManager = sessionDataManager;
		this.sessionLogoutHandlers = sessionLogoutHandlers.stream().collect(
				Collectors.toMap(ISessionLogoutHandler::getSessionType, Function.identity()));
	}
	
	public final void logoutAndDestroy(String authSessionType, String authSessionName) {
		sessionLogoutHandlers.get(authSessionType).logout(authSessionName);
		sessionDataManager.destroy(authSessionType, authSessionName);
	}
}
