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
package com.fortify.cli.common.session.manager.spi;

import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.api.ISessionTypeProvider;
import com.fortify.cli.common.session.manager.api.SessionLogoutManager;
import com.fortify.cli.common.session.manager.api.SessionDataManager;

import jakarta.inject.Inject;
import lombok.Getter;

public abstract class AbstractSessionHandlerAction<C> implements ISessionLoginHandler<C>, ISessionTypeProvider {
    @Getter @Inject private SessionDataManager sessionDataManager;
    @Inject private SessionLogoutManager sessionLogoutManager;
    
    public final void login(String authSessionName, C loginConfig) {
        logoutIfSessionExists(authSessionName);
        ISessionData authSessionData = _login(authSessionName, loginConfig);
        sessionDataManager.saveData(getSessionType(), authSessionName, authSessionData);
        testAuthenticatedConnection(authSessionName, loginConfig);
    }
    
    protected void testAuthenticatedConnection(String authSessionName, C loginConfig) {}

    private void logoutIfSessionExists(String authSessionName) {
        String sessionType = getSessionType();
        if ( sessionDataManager.exists(sessionType, authSessionName) ) {
            // Log out from previous session before creating a new session
            sessionLogoutManager.logoutAndDestroy(sessionType, authSessionName);
        }
    }

    protected abstract ISessionData _login(String authSessionName, C loginConfig);
}
