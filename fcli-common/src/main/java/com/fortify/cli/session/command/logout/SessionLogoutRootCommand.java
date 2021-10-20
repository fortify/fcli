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
package com.fortify.cli.session.command.logout;

import java.util.List;

import com.fortify.cli.command.RootCommand;
import com.fortify.cli.command.util.SubcommandOf;
import com.fortify.cli.session.LogoutHelper;
import com.fortify.cli.session.command.AbstractCommandWithLoginSessionHelper;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Singleton
@SubcommandOf(RootCommand.class)
@Command(name = "logout", description = "Logout from Fortify systems")
@Order(11)
public class SessionLogoutRootCommand extends AbstractCommandWithLoginSessionHelper implements Runnable {
	@Getter @Inject @ReflectiveAccess LogoutHelper logoutHelper;
	@Spec @ReflectiveAccess CommandSpec spec;
	
	@Option(names = {"--all", "-a"}, required = false, defaultValue = "false", scope = ScopeType.INHERIT)
	@Getter @ReflectiveAccess boolean logoutAll;
	
	@Override
	public void run() {
		if ( !logoutAll ) {
			throw new ParameterException(spec.commandLine(), "Either subcommand or --all flag must be given");
		} else {
			System.out.println("Deleting all login sessions");
			getLoginSessionHelper().listByLoginSessionType().forEach(this::logoutAndDestroy);
		}
	}
	
	private final void logoutAndDestroy(String loginSessionType, List<String> loginSessionNames) {
		loginSessionNames.forEach(loginSessionName->logoutAndDestroy(loginSessionType, loginSessionName));
	}

	private final void logoutAndDestroy(String loginSessionType, String loginSessionName) {
		logoutHelper.logoutAndDestroy(loginSessionType, loginSessionName);
	}
}
