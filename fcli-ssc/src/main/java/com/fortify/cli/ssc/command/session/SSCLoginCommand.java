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
package com.fortify.cli.ssc.command.session;

import com.fortify.cli.command.session.AbstractSessionLoginCommand;
import com.fortify.cli.command.session.LoginConnectionOptions;
import com.fortify.cli.command.session.LoginUserCredentialOptions;
import com.fortify.cli.command.session.SessionLoginRootCommand;
import com.fortify.cli.command.util.SubcommandOf;
import com.fortify.cli.ssc.rest.connection.SSCRestConnectionConfig;
import com.fortify.cli.ssc.rest.connection.SSCTokenRequest;
import com.fortify.cli.ssc.rest.connection.SSCTokenResponse;

import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Singleton
@SubcommandOf(SessionLoginRootCommand.class)
@Command(name = "ssc", description = "Login to SSC", sortOptions = false)
public class SSCLoginCommand extends AbstractSessionLoginCommand {
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC connection options:%n", order = 1)
	@Getter private SSCLoginConnectionOptions connectionOptions;
	
	@ArgGroup(exclusive = true, multiplicity = "1", order = 2)
    @Getter private SSCCredentials credentials;
	
	static class SSCLoginConnectionOptions extends LoginConnectionOptions {
		@Option(names = {"--allow-renew", "-r"}, description = "Allow SSC token renewal", order = 5) 
    	@Getter private boolean allowRenew;
	}
	
    static class SSCCredentials {
    	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC User credentials:%n", order = 3) 
    	@Getter private LoginUserCredentialOptions user = new LoginUserCredentialOptions();
    	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC Token credentials:%n", order = 4) 
    	@Getter private TokenCredentials token = new TokenCredentials();
    }
    
    static class TokenCredentials {
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
		SSCTokenRequest tokenRequest = SSCTokenRequest.builder().type("UnifiedLoginToken").build();
		UnirestInstance unirestInstance = getUnirestInstance();
		unirestInstance.config().defaultBaseUrl(config.getUrl());
		SSCTokenResponse tokenResponse = unirestInstance.post("/api/v1/tokens")
				.accept("application/json")
				.header("Content-Type", "application/json")
				.basicAuth(config.getUser(), new String(config.getPassword()))
				.body(tokenRequest)
				.asObject(SSCTokenResponse.class).getBody();
		return new SSCLoginSessionData(config, tokenResponse);
	}
	
	private final SSCRestConnectionConfig getRestConnectionConfig() {
		SSCRestConnectionConfig config = new SSCRestConnectionConfig();
		connectionOptions.configure(config);
		credentials.user.configure(config);
		config.setToken(credentials.token.getToken());
		config.setAllowRenew(connectionOptions.isAllowRenew());
		return config;
	}
}
