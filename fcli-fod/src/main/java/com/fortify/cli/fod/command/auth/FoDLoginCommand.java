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
package com.fortify.cli.fod.command.auth;

import com.fortify.cli.common.auth.ILoginHandler;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.auth.login.AbstractAuthLoginCommand;
import com.fortify.cli.common.picocli.command.auth.login.AuthLoginCommand;
import com.fortify.cli.common.picocli.command.auth.login.LoginConnectionOptions;
import com.fortify.cli.common.picocli.command.auth.login.LoginUserCredentialOptions;
import com.fortify.cli.fod.auth.FoDLoginHandler;
import com.fortify.cli.fod.rest.data.FoDClientCredentialsConfig;
import com.fortify.cli.fod.rest.data.FoDConnectionConfig;
import com.fortify.cli.fod.rest.data.FoDUserCredentialsConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@ReflectiveAccess
@SubcommandOf(AuthLoginCommand.class) 
@Command(name = ProductIdentifiers.FOD, description = "Login to FoD", sortOptions = false)
@RequiresProduct(ProductOrGroup.FOD)
public class FoDLoginCommand extends AbstractAuthLoginCommand<FoDConnectionConfig> {
	@Getter @Inject private FoDLoginHandler sscLoginHandler;
	
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "FoD connection options:%n", order = 1)
	@Getter private LoginConnectionOptions connectionOptions;
	
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "FoD authentication options:%n", order = 2)
    @Getter private FoDAuthOptions authOptions;
	
	static class FoDAuthOptions {
		@ArgGroup(exclusive = true, multiplicity = "1", order = 3)
	    @Getter private FoDCredentialOptions credentialOptions;
		@Option(names = {"--allow-renew", "-r"}, description = "Allow FoD token renewal", order = 4) 
    	@Getter private boolean renewAllowed;
	}
	
    static class FoDCredentialOptions {
    	@ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
    	@Getter private FoDUserCredentialOptions userOptions = new FoDUserCredentialOptions();
    	@ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
    	@Getter private FoDClientCredentialOptions clientCredentialOptions = new FoDClientCredentialOptions();
    }
    
    static class FoDUserCredentialOptions extends LoginUserCredentialOptions {
    	@Option(names = {"--tenant"}, required = true) 
    	@Getter private String tenant;
    	
    	public void configure(FoDUserCredentialsConfig config) {
    		super.configure(config);
    		config.setTenant(tenant);
    	}
    }
    
    static class FoDClientCredentialOptions {
    	@Option(names = {"--client-id"}, required = true) 
    	@Getter private String clientId;
    	@Option(names = {"--client-secret"}, required = true, interactive = true, arity = "0..1", echo = false) 
    	@Getter private String clientSecret;
    	
    	public void configure(FoDClientCredentialsConfig config) {
    		config.setClientId(clientId);
    		config.setClientSecret(clientSecret);
    	}
    }
	
	@Override
	protected String getAuthSessionType() {
		return ProductIdentifiers.FOD;
	}
	
	@Override
	protected final FoDConnectionConfig getConnectionConfig() {
		FoDConnectionConfig config = new FoDConnectionConfig();
		connectionOptions.configure(config.getNonNullBasicConnectionConfig());
		authOptions.getCredentialOptions().getUserOptions().configure(config.getNonNullUserCredentialsConfig());
		authOptions.getCredentialOptions().getClientCredentialOptions().configure(config.getNonNullClientCredentialsConfig());
		config.setRenewAllowed(authOptions.isRenewAllowed());
		return config;
	}

	@Override
	protected ILoginHandler<FoDConnectionConfig> getLoginHandler() {
		return sscLoginHandler;
	}
}
