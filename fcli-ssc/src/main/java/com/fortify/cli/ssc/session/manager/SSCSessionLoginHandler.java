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
package com.fortify.cli.ssc.session.manager;

import com.fortify.cli.common.rest.runner.IConnectionConfig;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.spi.AbstractSessionLoginHandler;
import com.fortify.cli.ssc.rest.runner.SSCUnauthenticatedUnirestRunner;
import com.fortify.cli.ssc.util.SSCConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;

@Singleton @ReflectiveAccess
public class SSCSessionLoginHandler extends AbstractSessionLoginHandler<SSCSessionLoginConfig> {
    @Inject private SSCUnauthenticatedUnirestRunner unauthenticatedUnirestRunner;
    @Inject private SSCSessionLogoutHandler logoutHandler;

    public final String getSessionType() {
        return SSCConstants.SESSION_TYPE;
    }
    
    @Override
    protected void _logoutBeforeNewLogin(String authSessionName, SSCSessionLoginConfig loginConfig) {
        logoutHandler.logout(authSessionName, loginConfig.getSscUserCredentialsConfig());
    }

    @Override
    public final ISessionData _login(String authSessionName, SSCSessionLoginConfig sscLoginConfig) {
        SSCSessionData sessionData = null;
        IConnectionConfig connectionConfig = sscLoginConfig.getConnectionConfig();
        if ( sscLoginConfig.getToken()!=null ) {
            sessionData = new SSCSessionData(sscLoginConfig);
        } else if ( sscLoginConfig.hasUserCredentialsConfig() ) {
            sessionData = unauthenticatedUnirestRunner.runWithUnirest(connectionConfig, unirest->generateSessionData(unirest, sscLoginConfig));
        } else {
            throw new IllegalArgumentException("Either SSC token or user credentials must be provided");
        }
        return sessionData;
    }
    
    private final SSCSessionData generateSessionData(UnirestInstance unirest, SSCSessionLoginConfig sscLoginConfig) {
        SSCTokenResponse sscTokenResponse = generateToken(unirest, sscLoginConfig.getSscUserCredentialsConfig());
        return new SSCSessionData(sscLoginConfig, sscTokenResponse);
    }
    
    private final SSCTokenResponse generateToken(UnirestInstance unirestInstance, ISSCUserCredentialsConfig sscUserCredentialsConfig) {
        SSCTokenRequest tokenRequest = SSCTokenRequest.builder()
                .type("UnifiedLoginToken")
                .terminalDate(sscUserCredentialsConfig.getExpiresAt())
                .build();
        return unirestInstance.post("/api/v1/tokens")
                .accept("application/json")
                .header("Content-Type", "application/json")
                .basicAuth(sscUserCredentialsConfig.getUser(), new String(sscUserCredentialsConfig.getPassword()))
                .body(tokenRequest)
                .asObject(SSCTokenResponse.class)
                .getBody();
    }
    
}
