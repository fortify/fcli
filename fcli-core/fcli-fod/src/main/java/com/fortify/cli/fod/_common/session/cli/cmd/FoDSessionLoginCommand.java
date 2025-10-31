/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod._common.session.cli.cmd;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;
import com.fortify.cli.fod._common.session.cli.mixin.FoDSessionLoginOptions;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
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
    @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplierMixin;
    
    @Override
    public ISessionNameSupplier getSessionNameSupplier() {
        return unirestInstanceSupplierMixin;
    }
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, FoDSessionDescriptor sessionDescriptor) {
        unirestInstanceSupplierMixin.close(sessionName);
        // TODO Can we revoke a previously generated FoD token?
    }

    @Override
    protected FoDSessionDescriptor login(String sessionName) {
        FoDSessionDescriptor sessionDescriptor;
        IUrlConfig urlConfig = loginOptions.getUrlConfigOptions();
        if ( loginOptions.hasClientCredentials() ) {
            FoDTokenCreateResponse createTokenResponse = FoDOAuthHelper.createToken(urlConfig, loginOptions.getClientCredentialOptions(), loginOptions.getAuthOptions().getScopes());
            sessionDescriptor = new FoDSessionDescriptor(urlConfig, createTokenResponse);
        } else if ( loginOptions.hasUserCredentials() ) {
            FoDTokenCreateResponse createTokenResponse = FoDOAuthHelper.createToken(urlConfig, loginOptions.getUserCredentials(), loginOptions.getAuthOptions().getScopes());
            sessionDescriptor = new FoDSessionDescriptor(urlConfig, createTokenResponse);
        } else {
            throw new FcliSimpleException("Either FoD client or user credentials must be provided");
        }
        return sessionDescriptor;
    }
}
