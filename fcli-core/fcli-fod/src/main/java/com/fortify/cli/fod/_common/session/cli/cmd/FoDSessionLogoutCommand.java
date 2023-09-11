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
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLogoutCommand;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Logout.CMD_NAME, sortOptions = false)
public class FoDSessionLogoutCommand extends AbstractSessionLogoutCommand<FoDSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Logout outputHelper;
    @Getter private FoDSessionHelper sessionHelper = FoDSessionHelper.instance();
    
    @Override
    protected void logout(String sessionName, FoDSessionDescriptor sessionDescriptor) {
        // FoD provides an undocumented /oauth/retireToken endpoint that we could potentially 
        // call here. However, it's not documented and FoD dev stated that it's not necessary
        // to explicitly retire tokens as they expire in 6 hours anyway. See the following issue
        // for more information: https://github.com/fortify/fcli/issues/422
    }
}
