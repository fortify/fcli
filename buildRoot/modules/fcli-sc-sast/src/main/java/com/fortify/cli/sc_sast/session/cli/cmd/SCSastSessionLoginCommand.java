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
package com.fortify.cli.sc_sast.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.sc_sast.rest.helper.SCSastUnirestHelper;
import com.fortify.cli.sc_sast.session.cli.mixin.SCSastSessionLoginOptions;
import com.fortify.cli.sc_sast.session.helper.SCSastSessionDescriptor;
import com.fortify.cli.sc_sast.session.helper.SCSastSessionHelper;
import com.fortify.cli.ssc.session.helper.ISSCCredentialsConfig;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class SCSastSessionLoginCommand extends AbstractSessionLoginCommand<SCSastSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private SCSastSessionHelper sessionHelper = SCSastSessionHelper.instance();
    @Mixin private SCSastSessionLoginOptions sessionLoginOptions;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, SCSastSessionDescriptor sessionData) {
        sessionData.logout(sessionLoginOptions.getCredentialOptions().getUserCredentialsConfig());
    }
    
    @Override
    protected SCSastSessionDescriptor login(String sessionName) {
        IUrlConfig urlConfig = sessionLoginOptions.getUrlConfigOptions();
        ISSCCredentialsConfig credentialsConfig = sessionLoginOptions.getCredentialOptions();
        return new SCSastSessionDescriptor(urlConfig, credentialsConfig, sessionLoginOptions.getClientAuthToken());
    }
    
    @Override
    protected void testAuthenticatedConnection(String sessionName) {
    	SCSastSessionDescriptor sessionData = SCSastSessionHelper.instance().get(sessionName, true);
    	try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
    	    testAuthenticatedConnection(unirest, sessionData);
    	}
    }

	private Void testAuthenticatedConnection(UnirestInstance u, SCSastSessionDescriptor sessionData) {
		SCSastUnirestHelper.configureScSastControllerUnirestInstance(u, sessionData);
		try {
			u.get("/rest/v2/ping").asString().getBody();
		} catch ( UnexpectedHttpResponseException e ) {
			if ( e.getStatus()==401 ) {
				throw new IllegalArgumentException("Error authenticating with SC SAST Controller; please check that --client-auth-token option value matches the client_auth_token as configured in config.properties on SC SAST Controller", e);
			} else {
				throw e;
			}
		}
		return null;
	}
}
