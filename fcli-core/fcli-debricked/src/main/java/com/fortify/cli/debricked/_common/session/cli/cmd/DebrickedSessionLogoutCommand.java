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
package com.fortify.cli.debricked._common.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLogoutCommand;
import com.fortify.cli.debricked._common.session.cli.mixin.DebrickedSessionNameArgGroup;
import com.fortify.cli.debricked._common.session.cli.mixin.DebrickedUnirestInstanceSupplierMixin;
import com.fortify.cli.debricked._common.session.helper.DebrickedSessionDescriptor;
import com.fortify.cli.debricked._common.session.helper.DebrickedSessionHelper;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Logout.CMD_NAME)
public class DebrickedSessionLogoutCommand extends AbstractSessionLogoutCommand<DebrickedSessionDescriptor> {
    @Getter @Mixin private OutputHelperMixins.Logout outputHelper;
    @Getter private DebrickedSessionHelper sessionHelper = DebrickedSessionHelper.instance();
    @Getter @ArgGroup(headingKey = "debricked.session.name.arggroup") 
    private DebrickedSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected void logout(String sessionName, DebrickedSessionDescriptor sessionDescriptor) {
        DebrickedUnirestInstanceSupplierMixin.shutdownUnirestInstance(sessionName);
        // Debricked JWT tokens are stateless and cannot be revoked on the server side
    }
}