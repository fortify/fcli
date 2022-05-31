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
package com.fortify.cli.common.picocli.command.session.login;

import com.fortify.cli.common.picocli.annotation.FixSuperclassInjection;
import com.fortify.cli.common.picocli.command.session.AbstractCommandWithSessionPersistenceHelper;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.common.session.login.ISessionLoginHandler;
import com.fortify.cli.common.session.summary.SessionSummaryHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@FixSuperclassInjection
public abstract class AbstractSessionLoginCommand<C> extends AbstractCommandWithSessionPersistenceHelper implements Runnable {
	@Inject private SessionSummaryHelper sessionSummaryHelper;
	@Mixin private OutputMixin outputMixin;
	
	@ArgGroup(heading = "Optional session name:%n", order = 1000)
    @Getter protected SessionNameOptions sessionNameOptions;
	
	@ReflectiveAccess
	private static class SessionNameOptions {
		@Option(names = {"--session-name", "-n"}, required = false, defaultValue = "default")
		@Getter protected String sessionName;
	}
	
	@Override @SneakyThrows
	public final void run() {
		getLoginHandler().login(getSessionName(), getLoginConfig());
		sessionSummaryHelper.writeSessionSummaries(getSessionType(), outputMixin);
	}
	
	public final String getSessionName() {
		return sessionNameOptions==null ? "default" : sessionNameOptions.getSessionName();
	}
	
	protected abstract String getSessionType();
	protected abstract C getLoginConfig();
	protected abstract ISessionLoginHandler<C> getLoginHandler();
}
