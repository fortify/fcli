/*
 * Copyright 2021-2026 Open Text.
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
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;
import com.fortify.cli.fod._common.session.cli.mixin.FoDSessionLoginSsoOptions;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;
import com.fortify.cli.fod._common.session.helper.oauth.FoDOAuthHelper;
import com.fortify.cli.fod._common.session.helper.sso.FoDSsoLoginHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "login-sso", sortOptions = false)
public class FoDSessionLoginSsoCommand extends AbstractSessionLoginCommand<FoDSessionDescriptor> {
    @Getter @Mixin private OutputHelperMixins.Login outputHelper;
    @Getter private FoDSessionHelper sessionHelper = FoDSessionHelper.instance();
    @Mixin private FoDSessionLoginSsoOptions loginOptions;
    @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplierMixin;

    @Override
    public ISessionNameSupplier getSessionNameSupplier() {
        return unirestInstanceSupplierMixin;
    }

    @Override
    protected void logoutBeforeNewLogin(String sessionName, FoDSessionDescriptor sessionDescriptor) {
        unirestInstanceSupplierMixin.close(sessionName);
    }

    @Override
    protected FoDSessionDescriptor login(String sessionName) {
        var urlConfig = loginOptions.getUrlConfig();
        var samlAssertion = loginOptions.hasSamlAssertionFile()
                ? FoDSsoLoginHelper.readSamlAssertion(loginOptions.getSamlAssertionFile())
                : FoDSsoLoginHelper.acquireSamlAssertion(loginOptions.getSsoUri(),
                        loginOptions.getBrowser(), loginOptions.getBrowserPath(), loginOptions.getBrowserProfile(),
                        loginOptions.getSsoTimeout());
        try {
            var tokenResponse = FoDOAuthHelper.createTokenFromSamlAssertion(urlConfig, samlAssertion);
            return new FoDSessionDescriptor(urlConfig, tokenResponse);
        } catch ( UnexpectedHttpResponseException e ) {
            if ( e.getStatus() == 400 || e.getStatus() == 401 ) {
                throw new FcliSimpleException("SSO authentication failed: FoD rejected the SAML assertion "
                        + "(check the tenant and that the SAML IdP URL belongs to this tenant)");
            }
            throw e;
        }
    }
}
