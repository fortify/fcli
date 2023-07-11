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
package com.fortify.cli.ssc.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.ssc.session.cli.mixin.SSCSessionLoginOptions;
import com.fortify.cli.ssc.session.helper.ISSCCredentialsConfig;
import com.fortify.cli.ssc.session.helper.SSCSessionDescriptor;
import com.fortify.cli.ssc.session.helper.SSCSessionHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class SSCSessionLoginCommand extends AbstractSessionLoginCommand<SSCSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private SSCSessionHelper sessionHelper = SSCSessionHelper.instance();
    @Mixin private SSCSessionLoginOptions sessionLoginOptions;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, SSCSessionDescriptor sessionDescriptor) {
        sessionDescriptor.logout(sessionLoginOptions.getUserCredentialsConfig());
    }
    
    @Override
    protected SSCSessionDescriptor login(String sessionName) {
        IUrlConfig urlConfig = sessionLoginOptions.getUrlConfigOptions();
        ISSCCredentialsConfig credentialsConfig = sessionLoginOptions.getCredentialOptions();
        return new SSCSessionDescriptor(urlConfig, credentialsConfig);
    }
}
