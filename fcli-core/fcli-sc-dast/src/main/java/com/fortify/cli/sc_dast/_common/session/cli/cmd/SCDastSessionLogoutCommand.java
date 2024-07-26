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
package com.fortify.cli.sc_dast._common.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLogoutCommand;
import com.fortify.cli.common.session.helper.SessionLogoutException;
import com.fortify.cli.sc_dast._common.session.cli.mixin.SCDastSessionLogoutOptions;
import com.fortify.cli.sc_dast._common.session.cli.mixin.SCDastUserCredentialOptions;
import com.fortify.cli.sc_dast._common.session.helper.SCDastSessionDescriptor;
import com.fortify.cli.sc_dast._common.session.helper.SCDastSessionHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Logout.CMD_NAME, sortOptions = false)
public class SCDastSessionLogoutCommand extends AbstractSessionLogoutCommand<SCDastSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Logout outputHelper;
    @Getter private SCDastSessionHelper sessionHelper = SCDastSessionHelper.instance();
    @Mixin private SCDastSessionLogoutOptions logoutOptions;
    
    @Override
    protected void logout(String sessionName, SCDastSessionDescriptor sessionDescriptor) {
        if ( !logoutOptions.isNoRevokeToken() ) {
            SCDastUserCredentialOptions userCredentialOptions = logoutOptions.getUserCredentialOptions();
            try {
                sessionDescriptor.logout(userCredentialOptions);
            } catch ( UnexpectedHttpResponseException e ) {
                if ( e.getStatus()==403 && userCredentialOptions==null ) {
                    throw new SessionLogoutException("SSC user credentials or --no-revoke-token option must be specified on SSC versions 23.2 or below", false);
                } else {
                    throw e;
                }
            }
        }
    }
}
