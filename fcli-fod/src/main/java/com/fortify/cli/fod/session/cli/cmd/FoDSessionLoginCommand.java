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
package com.fortify.cli.fod.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.rest.runner.GenericUnirestRunner;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.fod.oauth.helper.FoDOAuthHelper;
import com.fortify.cli.fod.oauth.helper.FoDTokenCreateResponse;
import com.fortify.cli.fod.session.cli.mixin.FoDSessionLoginOptions;
import com.fortify.cli.fod.session.manager.FoDSessionData;
import com.fortify.cli.fod.session.manager.FoDSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = BasicOutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class FoDSessionLoginCommand extends AbstractSessionLoginCommand<FoDSessionData> {
    @Getter @Mixin private BasicOutputHelperMixins.Login outputHelper;
    @Getter @Inject private FoDSessionDataManager sessionDataManager;
    @Getter @Inject private FoDOAuthHelper oauthHelper;
    @Inject private GenericUnirestRunner unirestRunner;
    @Mixin private FoDSessionLoginOptions loginOptions;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, FoDSessionData sessionData) {
        // TODO Can we revoke a previously generated FoD token?
    }

    @Override
    protected FoDSessionData login(String sessionName) {
        FoDSessionData sessionData;
        IUrlConfig urlConfig = loginOptions.getUrlConfigOptions();
        if ( loginOptions.hasClientCredentials() ) {
            FoDTokenCreateResponse createTokenResponse = oauthHelper.createToken(urlConfig, loginOptions.getClientCredentialOptions(), loginOptions.getScopes());
            sessionData = new FoDSessionData(urlConfig, createTokenResponse);
        } else if ( loginOptions.hasUserCredentialsConfig() ) {
            FoDTokenCreateResponse createTokenResponse = oauthHelper.createToken(urlConfig, loginOptions.getUserCredentialOptions(), loginOptions.getScopes());
            sessionData = new FoDSessionData(urlConfig, createTokenResponse);
        } else {
            throw new IllegalArgumentException("Either FoD client or user credentials must be provided");
        }
        return sessionData;
    }
}
