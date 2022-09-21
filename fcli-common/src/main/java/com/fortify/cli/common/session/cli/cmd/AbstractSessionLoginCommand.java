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
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.spi.ISessionDataManager;
import com.fortify.cli.common.util.FixInjection;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;

@ReflectiveAccess @FixInjection
public abstract class AbstractSessionLoginCommand<D extends ISessionData> implements Runnable {
    @Getter @Mixin private SessionNameMixin.OptionalParameter sessionNameMixin;
    @Mixin private OutputMixin outputMixin;
    
    @Override @SneakyThrows
    public final void run() {
        String sessionName = sessionNameMixin.getSessionName();
        ISessionDataManager<D> sessionDataManager = getSessionDataManager();
        logoutIfSessionExists(sessionName);
        D authSessionData = login(sessionName);
        sessionDataManager.save(sessionName, authSessionData);
        testAuthenticatedConnection(sessionName);
        sessionDataManager.writeSessionSummaries(outputMixin);
    }

    private void logoutIfSessionExists(String sessionName) {
        ISessionDataManager<D> sessionDataManager = getSessionDataManager();
        if ( sessionDataManager.exists(sessionName) ) {
            try {
                logoutBeforeNewLogin(sessionName, sessionDataManager.get(sessionName, false));
            } catch ( Exception e ) {
                // TODO Use proper logger, include exception details
                System.err.println("WARN: Error logging out previous session");
            } finally {
                sessionDataManager.destroy(sessionName);
            }
        }
    }

    protected abstract void logoutBeforeNewLogin(String sessionName, D sessionData);
    protected abstract D login(String sessionName);
    protected void testAuthenticatedConnection(String sessionName) {}
    protected abstract ISessionDataManager<D> getSessionDataManager();
}
