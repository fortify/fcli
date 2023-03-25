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

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.fod.session.cli.mixin.FoDSessionLoginOptions;
import com.fortify.cli.fod.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod.session.helper.FoDSessionHelper;
import com.fortify.cli.fod.session.helper.oauth.FoDOAuthHelper;
import com.fortify.cli.fod.session.helper.oauth.FoDTokenCreateResponse;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class FoDSessionLoginCommand extends AbstractSessionLoginCommand<FoDSessionDescriptor> {
    @Getter @Mixin private OutputHelperMixins.Login outputHelper;
    @Getter private FoDSessionHelper sessionHelper = FoDSessionHelper.instance();
    @Mixin private FoDSessionLoginOptions loginOptions;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, FoDSessionDescriptor sessionDescriptor) {
        // TODO Can we revoke a previously generated FoD token?
    }

    @Override
    protected FoDSessionDescriptor login(String sessionName) {
        FoDSessionDescriptor sessionDescriptor;
        IUrlConfig urlConfig = loginOptions.getUrlConfigOptions();
        if ( loginOptions.hasClientCredentials() ) {
            FoDTokenCreateResponse createTokenResponse = FoDOAuthHelper.createToken(urlConfig, loginOptions.getClientCredentialOptions(), loginOptions.getScopes());
            sessionDescriptor = new FoDSessionDescriptor(urlConfig, createTokenResponse);
        } else if ( loginOptions.hasUserCredentialsConfig() ) {
            FoDTokenCreateResponse createTokenResponse = FoDOAuthHelper.createToken(urlConfig, loginOptions.getUserCredentialOptions(), loginOptions.getScopes());
            sessionDescriptor = new FoDSessionDescriptor(urlConfig, createTokenResponse);
        } else {
            throw new IllegalArgumentException("Either FoD client or user credentials must be provided");
        }
        return sessionDescriptor;
    }
}
