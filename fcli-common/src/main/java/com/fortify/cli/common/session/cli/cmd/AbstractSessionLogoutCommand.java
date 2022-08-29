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
package com.fortify.cli.common.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.session.manager.api.ISessionTypeProvider;
import com.fortify.cli.common.session.manager.api.SessionDataManager;
import com.fortify.cli.common.session.manager.api.SessionLogoutManager;
import com.fortify.cli.common.session.manager.api.SessionSummaryManager;
import com.fortify.cli.common.util.FixInjection;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess @FixInjection
public abstract class AbstractSessionLogoutCommand implements Runnable, ISessionTypeProvider {
    @Inject private SessionDataManager sessionDataManager;
    @Getter @Inject private SessionLogoutManager sessionLogoutManager;
    @Getter @Mixin private SessionNameMixin sessionNameMixin;
    @Inject private SessionSummaryManager sessionSummaryManager;
    @Mixin private OutputMixin outputMixin;

    @Override
    public final void run() {
        sessionLogoutManager.logoutAndDestroy(getSessionType(), sessionNameMixin.getSessionName());
        sessionSummaryManager.writeSessionSummaries(getSessionType(), outputMixin);
    }
}
