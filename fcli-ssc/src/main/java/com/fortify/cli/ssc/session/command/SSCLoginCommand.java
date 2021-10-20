/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.session.command;

import java.util.Arrays;

import com.fortify.cli.command.util.SubcommandOf;
import com.fortify.cli.rest.connection.UnirestInstanceFactory;
import com.fortify.cli.session.command.login.AbstractSessionLoginCommand;
import com.fortify.cli.session.command.login.LoginConnectionOptions;
import com.fortify.cli.session.command.login.LoginUserCredentialOptions;
import com.fortify.cli.session.command.login.SessionLoginRootCommand;
import com.fortify.cli.ssc.rest.connection.SSCRestConnectionConfig;
import com.fortify.cli.ssc.rest.connection.SSCRestConnectionConfig.SSCAuthType;
import com.fortify.cli.ssc.rest.connection.SSCTokenRequest;
import com.fortify.cli.ssc.rest.connection.SSCTokenResponse;
import com.fortify.cli.ssc.session.SSCLoginSessionData;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Singleton
@SubcommandOf(SessionLoginRootCommand.class)
@Command(name = "ssc", description = "Login to SSC", sortOptions = false)
public class SSCLoginCommand extends AbstractSessionLoginCommand {
	@Getter @Setter(onMethod_= {@Inject}) private UnirestInstanceFactory unirestInstanceFactory;
	
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC connection options:%n", order = 1)
	@Getter private LoginConnectionOptions connectionOptions;
	
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC authentication options:%n", order = 2)
    @Getter private SSCAuthOptions authOptions;
	
	static class SSCAuthOptions {
		@ArgGroup(exclusive = true, multiplicity = "1", order = 3)
	    @Getter private SSCCredentialOptions credentialOptions;
		@Option(names = {"--allow-renew", "-r"}, description = "Allow SSC token renewal", order = 4) 
    	@Getter private boolean allowRenew;
	}
	
    static class SSCCredentialOptions {
    	@ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
    	@Getter private LoginUserCredentialOptions userOptions = new LoginUserCredentialOptions();
    	@ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
    	@Getter private TokenOptions tokenOptions = new TokenOptions();
    }
    
    static class TokenOptions {
    	@Option(names = {"--token", "-t"}, required = true, interactive = true, arity = "0..1", echo = false) 
    	@Getter private char[] token;
    }
	
	@Override
	protected String getLoginSessionType() {
		return "ssc";
	}
	
	@Override
	protected Object login() {
		SSCRestConnectionConfig config = getRestConnectionConfig();
		SSCTokenResponse tokenResponse = null;
		if ( config.getAuthType()==SSCAuthType.USER ) {
			tokenResponse = authenticateWithUserCredentials(config);
			if ( !config.isAllowRenew() ) {
				clearUserCredentials(config);
			}
		}
		return new SSCLoginSessionData(config, tokenResponse);
	}

	private void clearUserCredentials(SSCRestConnectionConfig config) {
		Arrays.fill(config.getPassword(), 'x');
		config.setPassword(null);
		config.setUser(null);
	}
	
	private SSCTokenResponse authenticateWithUserCredentials(SSCRestConnectionConfig config) {
		SSCTokenRequest tokenRequest = SSCTokenRequest.builder().type("UnifiedLoginToken").build();
		UnirestInstance unirestInstance = unirestInstanceFactory.getUnirestInstance(getConnectionId());
		unirestInstance.config().defaultBaseUrl(config.getUrl());
		return unirestInstance.post("/api/v1/tokens")
				.accept("application/json")
				.header("Content-Type", "application/json")
				.basicAuth(config.getUser(), new String(config.getPassword()))
				.body(tokenRequest)
				.asObject(SSCTokenResponse.class).getBody();
	}

	private String getConnectionId() {
		return String.format("%s/%s", getLoginSessionType(), getLoginSessionName());
	}

	private final SSCRestConnectionConfig getRestConnectionConfig() {
		SSCRestConnectionConfig config = new SSCRestConnectionConfig();
		connectionOptions.configure(config);
		authOptions.getCredentialOptions().getUserOptions().configure(config);
		config.setToken(authOptions.getCredentialOptions().getTokenOptions().getToken());
		config.setAllowRenew(authOptions.isAllowRenew());
		return config;
	}
}
