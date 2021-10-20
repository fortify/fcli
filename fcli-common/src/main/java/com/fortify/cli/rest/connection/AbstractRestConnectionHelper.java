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
package com.fortify.cli.rest.connection;

import com.fortify.cli.session.ILogoutManager;
import com.fortify.cli.session.LoginSessionHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;

public abstract class AbstractRestConnectionHelper<D> implements ILogoutManager {
	@Getter @Inject @ReflectiveAccess private UnirestInstanceFactory unirestInstanceFactory;
	@Getter @Inject @ReflectiveAccess private LoginSessionHelper loginSessionHelper;
	
	@Override
	public final void logout(String loginSessionName) {
		logout(getLoginSessionData(loginSessionName));
	}

	private D getLoginSessionData(String loginSessionName) {
		return loginSessionHelper.getData(getLoginSessionType(), loginSessionName, getSessionDataClass());
	}
	
	protected abstract Class<D> getSessionDataClass();

	/**
	 * This method may be overridden by concrete implementations to log out from the target system.
	 * For example, this could terminate the current session or invalidate a temporary authentication token.
	 * @param sessionData Data identifying the login session to be logged out
	 */
	public void logout(D sessionData) {}
}
