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
package com.fortify.cli.ssc._common.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLogoutCommand;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.session.helper.FcliSessionLogoutException;
import com.fortify.cli.ssc._common.rest.cli.mixin.SSCAndScanCentralUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCAndScanCentralSessionLogoutOptions;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Logout.CMD_NAME, sortOptions = false)
public class SSCSessionLogoutCommand extends AbstractSessionLogoutCommand<SSCAndScanCentralSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Logout outputHelper;
    @Getter private SSCAndScanCentralSessionHelper sessionHelper = SSCAndScanCentralSessionHelper.instance();
    @Mixin private SSCAndScanCentralSessionLogoutOptions logoutOptions;
    @Mixin private SSCAndScanCentralUnirestInstanceSupplierMixin unirestInstanceSupplierMixin;
    
    @Override
    public ISessionNameSupplier getSessionNameSupplier() {
    return unirestInstanceSupplierMixin;
    }
    
    @Override
    protected void logout(String sessionName, SSCAndScanCentralSessionDescriptor sessionDescriptor) {
        unirestInstanceSupplierMixin.close(sessionName);
        if ( !logoutOptions.isNoRevokeToken() ) {
            UserCredentialOptions userCredentialOptions = logoutOptions.getUserCredentialOptions();
            try {
                sessionDescriptor.logout(userCredentialOptions);
            } catch ( UnexpectedHttpResponseException e ) {
                if ( e.getStatus()==403 && userCredentialOptions==null ) {
                    throw new FcliSessionLogoutException("SSC user credentials or --no-revoke-token option must be specified on SSC versions 23.2 or below", false);
                } else {
                    throw e;
                }
            }
        }
    }
}
