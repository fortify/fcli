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
package com.fortify.cli.ssc._common.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLogoutCommand;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCSessionLogoutOptions;
import com.fortify.cli.ssc._common.session.helper.SSCSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCSessionHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Logout.CMD_NAME, sortOptions = false)
public class SSCSessionLogoutCommand extends AbstractSessionLogoutCommand<SSCSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Logout outputHelper;
    @Getter private SSCSessionHelper sessionHelper = SSCSessionHelper.instance();
    @Mixin private SSCSessionLogoutOptions logoutOptions;
    
    @Override
    protected void logout(String sessionName, SSCSessionDescriptor sessionDescriptor) {
        sessionDescriptor.logout(logoutOptions.getUserCredentialOptions());
    }
}
