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
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;
import com.fortify.cli.fod._common.session.cli.mixin.FoDSessionLoginOptions;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;
import com.fortify.cli.fod._common.session.helper.oauth.FoDOAuthHelper;
import com.fortify.cli.fod._common.session.helper.oauth.FoDTokenCreateResponse;
import com.fortify.cli.fod._common.session.helper.oauth.impl.BasicFoDUserAuthCode;
import com.fortify.cli.fod._common.session.helper.oauth.impl.BasicFoDUserCredentials;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false, preprocessor = FoDSessionTenantIgnoringPreprocessor.class)
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
        if (loginOptions.hasClientCredentials()) {
            FoDTokenCreateResponse createTokenResponse = FoDOAuthHelper.createToken(urlConfig, loginOptions.getClientCredentialOptions(), loginOptions.getAuthOptions().getScopes());
            sessionDescriptor = new FoDSessionDescriptor(urlConfig, createTokenResponse);
        } else if (loginOptions.hasUserCredentials()) {
            var credBuilder = BasicFoDUserCredentials.builder()
                    .tenant(loginOptions.getUserCredentialOptions().getTenant())
                    .user(loginOptions.getUserCredentialOptions().getUser())
                    .password(loginOptions.getUserCredentialOptions().getPassword());

            var authCodeBuilder = BasicFoDUserAuthCode.builder()
                    .securityCode(loginOptions.getUserCredentialOptions().getSecurityCode())
                    .isTotp(loginOptions.getUserCredentialOptions().isTotp());
            try {
                FoDTokenCreateResponse createTokenResponse = FoDOAuthHelper.createToken(urlConfig, credBuilder.build(),
                        authCodeBuilder.build(), loginOptions.getAuthOptions().getScopes());
                sessionDescriptor = new FoDSessionDescriptor(urlConfig, createTokenResponse);
            } catch (UnexpectedHttpResponseException e) {
                if (e.getStatus() == 400) {
                    String mfaGuidance = "If MFA is required, provide the security code:\n"
                            + "  --code <code>  (or -c <code>) to provide the security code\n"
                            + "  --totp          to indicate the code is from a TOTP authenticator app";

                    String errorWithCode = "Authentication failed. Possible causes:\n"
                            + "  - Incorrect username or password\n"
                            + "  - MFA/TOTP code incorrect, expired, or wrong type (TOTP vs MFA)\n"
                            + "Please verify your credentials and MFA/TOTP code if applicable:\n"
                            + mfaGuidance;

                    String errorWithoutCode = "Authentication failed. Possible causes:\n" +
                            "  - Incorrect username or password\n" +
                            "  - FoD tenant requires MFA/TOTP authentication\n\n" +
                            mfaGuidance;
                    String errorMessage = loginOptions.hasSecurityCode() ? errorWithCode : errorWithoutCode;
                    throw new FcliSimpleException(errorMessage);
                }
                throw new FcliTechnicalException(e.getMessage(), e);
            }
        } else {
            throw new FcliSimpleException("Either FoD client or user credentials must be provided");
        }
        return sessionDescriptor;
    }
}
