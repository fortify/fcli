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
package com.fortify.cli.session.command.login;

import com.fortify.cli.session.LogoutHelper;
import com.fortify.cli.session.command.AbstractCommandWithLoginSessionHelper;

import jakarta.inject.Inject;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public abstract class AbstractSessionLoginCommand extends AbstractCommandWithLoginSessionHelper implements Runnable {
	@Getter @Inject LogoutHelper logoutHelper;
	
	@ArgGroup(heading = "Optional login session name:%n", order = 1000)
    @Getter private LoginSessionNameOptions loginSessionNameOptions;
	
	private static class LoginSessionNameOptions {
		@Option(names = {"--login-session-name", "-n"}, required = false, defaultValue = "default")
		@Getter private String loginSessionName;
	}
	
	@Override @SneakyThrows
	public final void run() {
		String loginSessionType = getLoginSessionType();
		String loginSessionName = getLoginSessionName();
		if ( getLoginSessionHelper().exists(loginSessionType, loginSessionName) ) {
			// Log out from previous session before creating a new session
			getLogoutHelper().logoutAndDestroy(loginSessionType, loginSessionName);
		}
		Object loginSessionData = login();
		getLoginSessionHelper().saveData(loginSessionType, loginSessionName, loginSessionData);
	}
	
	public final String getLoginSessionName() {
		return loginSessionNameOptions==null ? "default" : loginSessionNameOptions.getLoginSessionName();
	}
	
	protected abstract String getLoginSessionType();
	protected abstract Object login();
}
