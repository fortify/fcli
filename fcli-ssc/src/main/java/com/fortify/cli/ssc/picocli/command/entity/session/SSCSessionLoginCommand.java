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
package com.fortify.cli.ssc.picocli.command.entity.session;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.command.session.login.AbstractSessionLoginCommand;
import com.fortify.cli.common.picocli.command.session.login.LoginConnectionOptions;
import com.fortify.cli.common.picocli.command.session.login.LoginUserCredentialOptions;
import com.fortify.cli.common.session.login.ISessionLoginHandler;
import com.fortify.cli.common.time.DateTimeHelper;
import com.fortify.cli.ssc.session.login.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.session.login.SSCSessionLoginConfig;
import com.fortify.cli.ssc.session.login.SSCSessionLoginHandler;

import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

@Command(name = "login", description = "Login to SSC", sortOptions = false)
public class SSCSessionLoginCommand extends AbstractSessionLoginCommand<SSCSessionLoginConfig> {
	@Getter @Inject private SSCSessionLoginHandler sscLoginHandler;
	
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC connection options:%n", order = 1)
	@Getter private LoginConnectionOptions connectionOptions;
	
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC authentication options:%n", order = 2)
    @Getter private SSCAuthOptions authOptions;
	
	static class SSCAuthOptions {
		@ArgGroup(exclusive = true, multiplicity = "1", order = 3)
	    @Getter private SSCCredentialOptions credentialOptions;
	}
	
    static class SSCCredentialOptions {
    	@ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
    	@Getter private SSCUserCredentialOptions userOptions = new SSCUserCredentialOptions();
    	@ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
    	@Getter private TokenOptions tokenOptions = new TokenOptions();
    }
    
    static class SSCUserCredentialOptions extends LoginUserCredentialOptions implements ISSCUserCredentialsConfig {
    	@Option(names = {"--expire-in"}, required = false, defaultValue = "1d", showDefaultValue = Visibility.ALWAYS) 
    	@Getter private String expireIn;
    	
    	@Override
    	public OffsetDateTime getExpiresAt() {
    		return DateTimeHelper.getCurrentOffsetDateTimePlusPeriod(expireIn);
    	}
    }
    
    static class TokenOptions {
    	@Option(names = {"--token", "-t"}, required = true, interactive = true, arity = "0..1", echo = false) 
    	@Getter private char[] token;
    }
	
	@Override
	protected String getSessionType() {
		return ProductIdentifiers.SSC;
	}
	
	@Override
	protected final SSCSessionLoginConfig getLoginConfig() {
		SSCSessionLoginConfig config = new SSCSessionLoginConfig();
		config.setConnectionConfig(getConnectionOptions());
		Optional.ofNullable(authOptions).map(SSCAuthOptions::getCredentialOptions).map(SSCCredentialOptions::getUserOptions).ifPresent(config::setSscUserCredentialsConfig);
		Optional.ofNullable(authOptions).map(SSCAuthOptions::getCredentialOptions).map(SSCCredentialOptions::getTokenOptions).map(TokenOptions::getToken).ifPresent(config::setToken);
		return config;
	}
	
	@Override
	protected ISessionLoginHandler<SSCSessionLoginConfig> getLoginHandler() {
		return sscLoginHandler;
	}
}
