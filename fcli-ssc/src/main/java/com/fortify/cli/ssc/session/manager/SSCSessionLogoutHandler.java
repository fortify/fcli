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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.session.manager.api.IUserCredentials;
import com.fortify.cli.common.session.manager.api.SessionDataManager;
import com.fortify.cli.common.session.manager.spi.AbstractSessionLogoutHandler;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.runner.SSCAuthenticatedUnirestRunner;
import com.fortify.cli.ssc.util.SSCConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@Singleton @ReflectiveAccess
public class SSCSessionLogoutHandler extends AbstractSessionLogoutHandler<IUserCredentials> {
    @Getter @Inject private SessionDataManager sessionDataManager;
    @Getter @Inject private SSCAuthenticatedUnirestRunner unirestRunner;

    @Override
    public final void _logout(String authSessionName, IUserCredentials uc) {
        SSCSessionData data = sessionDataManager.getData(getSessionType(), authSessionName, SSCSessionData.class);
        String tokenId = data==null ? null : data.getTokenId();
        if ( tokenId!=null && uc!=null ) {
            unirestRunner.runWithUnirest(authSessionName, unirestInstance->logout(unirestInstance, uc, tokenId));
        }
    }
    
    private final Void logout(UnirestInstance unirestInstance, IUserCredentials uc, String tokenId) {
        try {
            unirestInstance.delete(SSCUrls.TOKENS)
                .queryString("ids", tokenId)
                .basicAuth(uc.getUser(), new String(uc.getPassword()))
                .asObject(JsonNode.class).getBody();
        } catch ( RuntimeException e ) {
            // TODO Log warning instead of printing to stdout
            System.out.println("Error revoking token:" + e.getMessage());
        }
        return null;
    }

    @Override
    public String getSessionType() {
        return SSCConstants.SESSION_TYPE;
    }
}
