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
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.sc_dast._common.session.cli.mixin.SCDastSessionLoginOptions;
import com.fortify.cli.sc_dast._common.session.helper.SCDastSessionDescriptor;
import com.fortify.cli.sc_dast._common.session.helper.SCDastSessionHelper;
import com.fortify.cli.ssc._common.session.helper.ISSCCredentialsConfig;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class SCDastSessionLoginCommand extends AbstractSessionLoginCommand<SCDastSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private SCDastSessionHelper sessionHelper = SCDastSessionHelper.instance();
    @Mixin private SCDastSessionLoginOptions sessionLoginOptions;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, SCDastSessionDescriptor sessionDescriptor) {
        sessionDescriptor.logout(sessionLoginOptions.getCredentialOptions().getUserCredentialsConfig());
    }
    
    @Override
    protected SCDastSessionDescriptor login(String sessionName) {
        IUrlConfig urlConfig = sessionLoginOptions.getUrlConfigOptions();
        ISSCCredentialsConfig credentialsConfig = sessionLoginOptions.getCredentialOptions();
        return new SCDastSessionDescriptor(urlConfig, credentialsConfig);
    }
}
