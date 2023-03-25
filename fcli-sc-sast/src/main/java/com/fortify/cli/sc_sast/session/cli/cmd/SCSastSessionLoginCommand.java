/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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
