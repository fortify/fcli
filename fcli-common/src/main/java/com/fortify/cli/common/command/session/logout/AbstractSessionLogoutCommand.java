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
package com.fortify.cli.common.command.session.logout;

import com.fortify.cli.common.command.session.AbstractCommandWithLoginSessionHelper;
import com.fortify.cli.common.command.session.consumer.LoginSessionConsumerMixin;
import com.fortify.cli.common.session.LogoutHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParentCommand;

@ReflectiveAccess
public abstract class AbstractSessionLogoutCommand extends AbstractCommandWithLoginSessionHelper implements Runnable {
	@Getter @Inject private LogoutHelper logoutHelper;
	@Getter @Mixin private LoginSessionConsumerMixin loginSessionConsumerMixin;
	@ParentCommand private RootLogoutCommand parent;
	
	@Override
	public final void run() {
		if ( parent.isLogoutAll() ) {
			System.out.println(String.format("Logging out all %s sessions", getLoginSessionType()));
			getLoginSessionHelper().list(getLoginSessionType()).forEach(this::logoutAndDestroy);
		} else {
			String loginSessionName = loginSessionConsumerMixin.getLoginSessionName();
			logoutAndDestroy(loginSessionName);
		}
	}
	
	private final void logoutAndDestroy(String loginSessionName) {
		String loginSessionType = getLoginSessionType();
		logoutHelper.logoutAndDestroy(loginSessionType, loginSessionName);
	}
	
	protected abstract String getLoginSessionType();
}
