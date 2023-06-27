/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_sast.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLogoutCommand;
import com.fortify.cli.sc_sast.session.cli.mixin.SCSastSessionLogoutOptions;
import com.fortify.cli.sc_sast.session.helper.SCSastSessionDescriptor;
import com.fortify.cli.sc_sast.session.helper.SCSastSessionHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Logout.CMD_NAME, sortOptions = false)
public class SCSastSessionLogoutCommand extends AbstractSessionLogoutCommand<SCSastSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Logout outputHelper;
    @Getter private SCSastSessionHelper sessionHelper = SCSastSessionHelper.instance();
    @Mixin private SCSastSessionLogoutOptions logoutOptions;
    
    @Override
    protected void logout(String sessionName, SCSastSessionDescriptor sessionData) {
        sessionData.logout(logoutOptions.getUserCredentialOptions());
    }
}
