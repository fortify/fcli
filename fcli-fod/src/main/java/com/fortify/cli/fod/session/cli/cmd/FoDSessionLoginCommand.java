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
package com.fortify.cli.fod.session.cli.cmd;

import java.util.Optional;

import com.fortify.cli.common.session.cli.AbstractSessionLoginCommand;
import com.fortify.cli.common.session.cli.LoginConnectionOptions;
import com.fortify.cli.common.session.cli.LoginUserCredentialOptions;
import com.fortify.cli.common.session.manager.spi.ISessionLoginHandler;
import com.fortify.cli.fod.session.manager.FoDSessionLoginConfig;
import com.fortify.cli.fod.session.manager.FoDSessionLoginHandler;
import com.fortify.cli.fod.session.manager.IFoDClientCredentialsConfig;
import com.fortify.cli.fod.session.manager.IFoDUserCredentialsConfig;
import com.fortify.cli.fod.util.FoDConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "login", sortOptions = false)
public class FoDSessionLoginCommand extends AbstractSessionLoginCommand<FoDSessionLoginConfig> {
	@Getter @Inject private FoDSessionLoginHandler fodLoginHandler;
	
	@ArgGroup(exclusive = false, multiplicity = "1", headingKey = "arggroup.fod-connection-options.heading", order = 1)
	@Getter private LoginConnectionOptions connectionOptions;
	
	@ArgGroup(exclusive = false, multiplicity = "1", headingKey = "arggroup.fod-authentication-options.heading", order = 2)
    @Getter private FoDAuthOptions authOptions;
	
	static class FoDAuthOptions {
		@ArgGroup(exclusive = true, multiplicity = "1", order = 3)
	    @Getter private FoDCredentialOptions credentialOptions;
	}
	
    static class FoDCredentialOptions {
    	@ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
    	@Getter private FoDUserCredentialOptions userCredentialOptions = new FoDUserCredentialOptions();
    	@ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
    	@Getter private FoDClientCredentialOptions clientCredentialOptions = new FoDClientCredentialOptions();
    }
    
    static class FoDUserCredentialOptions extends LoginUserCredentialOptions implements IFoDUserCredentialsConfig {
    	@Option(names = {"--tenant"}, required = true) 
    	@Getter private String tenant;
    }
    
    static class FoDClientCredentialOptions implements IFoDClientCredentialsConfig {
    	@Option(names = {"--client-id"}, required = true) 
    	@Getter private String clientId;
    	@Option(names = {"--client-secret"}, required = true, interactive = true, arity = "0..1", echo = false) 
    	@Getter private String clientSecret;
    }
	
	@Override
	protected String getSessionType() {
		return FoDConstants.SESSION_TYPE;
	}
	
	@Override
	protected final FoDSessionLoginConfig getLoginConfig() {
		FoDSessionLoginConfig config = new FoDSessionLoginConfig();
		config.setConnectionConfig(getConnectionOptions());
		Optional.ofNullable(authOptions).map(FoDAuthOptions::getCredentialOptions).map(FoDCredentialOptions::getUserCredentialOptions).ifPresent(config::setFodUserCredentialsConfig);
		Optional.ofNullable(authOptions).map(FoDAuthOptions::getCredentialOptions).map(FoDCredentialOptions::getClientCredentialOptions).ifPresent(config::setFodClientCredentialsConfig);
		return config;
	}

	@Override
	protected ISessionLoginHandler<FoDSessionLoginConfig> getLoginHandler() {
		return fodLoginHandler;
	}
}
