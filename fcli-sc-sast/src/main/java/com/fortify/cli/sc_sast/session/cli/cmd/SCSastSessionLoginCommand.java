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

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.rest.runner.GenericUnirestRunner;
import com.fortify.cli.common.rest.runner.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_sast.rest.helper.SCSastUnirestHelper;
import com.fortify.cli.sc_sast.session.cli.mixin.SCSastSessionLoginOptions;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionDataManager;
import com.fortify.cli.ssc.session.manager.ISSCCredentialsConfig;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = BasicOutputHelperMixins.Login.CMD_NAME, sortOptions = false)
@FixInjection
public class SCSastSessionLoginCommand extends AbstractSessionLoginCommand<SCSastSessionData> {
    @Getter @Mixin private BasicOutputHelperMixins.Login outputHelper;
    @Getter @Inject private SCSastSessionDataManager sessionDataManager;
    @Inject private GenericUnirestRunner unirestRunner;
    @Inject private SSCTokenHelper tokenHelper;
    @Mixin private SCSastSessionLoginOptions sessionLoginOptions;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, SCSastSessionData sessionData) {
        sessionData.logout(tokenHelper, sessionLoginOptions.getCredentialOptions().getUserCredentialsConfig());
    }
    
    @Override
    protected SCSastSessionData login(String sessionName) {
        IUrlConfig urlConfig = sessionLoginOptions.getUrlConfigOptions();
        ISSCCredentialsConfig credentialsConfig = sessionLoginOptions.getCredentialOptions();
        return new SCSastSessionData(urlConfig, credentialsConfig, sessionLoginOptions.getClientAuthToken(), tokenHelper);
    }
    
    @Override
    protected void testAuthenticatedConnection(String sessionName) {
    	SCSastSessionData sessionData = sessionDataManager.get(sessionName, true);
    	unirestRunner.run(u->testAuthenticatedConnection(u, sessionData));
    }

	private Void testAuthenticatedConnection(UnirestInstance u, SCSastSessionData sessionData) {
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
