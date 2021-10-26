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

import com.fortify.cli.common.command.session.login.AbstractSessionLoginCommand;
import com.fortify.cli.common.command.session.login.LoginConnectionOptions;
import com.fortify.cli.common.command.session.login.LoginUserCredentialOptions;
import com.fortify.cli.common.command.session.login.RootLoginCommand;
import com.fortify.cli.common.command.util.annotation.RequiresProduct;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.config.product.Product;
import com.fortify.cli.common.config.product.Product.ProductIdentifiers;
import com.fortify.cli.common.session.ILoginHandler;
import com.fortify.cli.ssc.rest.data.SSCConnectionConfig;
import com.fortify.cli.ssc.rest.unirest.SSCLoginHandler;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@ReflectiveAccess
@SubcommandOf(RootLoginCommand.class) 
@Command(name = ProductIdentifiers.SSC, description = "Login to SSC", sortOptions = false)
@RequiresProduct(Product.SSC)
public class SSCLoginCommand extends AbstractSessionLoginCommand<SSCConnectionConfig> {
	@Getter @Inject private SSCLoginHandler sscLoginHandler;
	
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC connection options:%n", order = 1)
	@Getter private LoginConnectionOptions connectionOptions;
	
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC authentication options:%n", order = 2)
    @Getter private SSCAuthOptions authOptions;
	
	static class SSCAuthOptions {
		@ArgGroup(exclusive = true, multiplicity = "1", order = 3)
	    @Getter private SSCCredentialOptions credentialOptions;
		@Option(names = {"--allow-renew", "-r"}, description = "Allow SSC token renewal", order = 4) 
    	@Getter private boolean renewAllowed;
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
		return ProductIdentifiers.SSC;
	}
	
	@Override
	protected final SSCConnectionConfig getConnectionConfig() {
		SSCConnectionConfig config = new SSCConnectionConfig();
		connectionOptions.configure(config.getNonNullBasicConnectionConfig());
		authOptions.getCredentialOptions().getUserOptions().configure(config.getNonNullBasicUserCredentialsConfig());
		config.setToken(authOptions.getCredentialOptions().getTokenOptions().getToken());
		config.setRenewAllowed(authOptions.isRenewAllowed());
		return config;
	}

	@Override
	protected ILoginHandler<SSCConnectionConfig> getLoginHandler() {
		return sscLoginHandler;
	}
}
