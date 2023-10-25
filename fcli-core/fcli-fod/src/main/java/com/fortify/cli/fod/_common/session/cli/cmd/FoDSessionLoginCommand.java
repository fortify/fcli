/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod._common.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.fod._common.session.cli.mixin.FoDSessionLoginOptions;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;
import com.fortify.cli.fod._common.session.helper.oauth.FoDOAuthHelper;
import com.fortify.cli.fod._common.session.helper.oauth.FoDTokenCreateResponse;

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
            FoDTokenCreateResponse createTokenResponse = FoDOAuthHelper.createToken(urlConfig, loginOptions.getClientCredentialOptions(), loginOptions.getAuthOptions().getScopes());
            sessionDescriptor = new FoDSessionDescriptor(urlConfig, createTokenResponse);
        } else if ( loginOptions.hasUserCredentialsConfig() ) {
            FoDTokenCreateResponse createTokenResponse = FoDOAuthHelper.createToken(urlConfig, loginOptions.getUserCredentialOptions(), loginOptions.getAuthOptions().getScopes());
            sessionDescriptor = new FoDSessionDescriptor(urlConfig, createTokenResponse);
        } else {
            throw new IllegalArgumentException("Either FoD client or user credentials must be provided");
        }
        return sessionDescriptor;
    }
}
