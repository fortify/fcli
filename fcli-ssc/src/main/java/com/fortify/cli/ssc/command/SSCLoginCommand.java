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
package com.fortify.cli.ssc.command;

import com.fortify.cli.command.session.LoginConnectionOptions;
import com.fortify.cli.command.session.LoginSessionAliasMixin;
import com.fortify.cli.command.session.LoginUserCredentialOptions;
import com.fortify.cli.command.session.SessionLoginRootCommand;
import com.fortify.cli.command.util.SubcommandOf;

import jakarta.inject.Singleton;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Singleton
@SubcommandOf(SessionLoginRootCommand.class)
@Command(name = "ssc", description = "Login to SSC", sortOptions = false)
public class SSCLoginCommand implements Runnable {
	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC connection options:%n", order = 1)
	@Getter private LoginConnectionOptions conn;
	
	@ArgGroup(exclusive = true, multiplicity = "1", order = 2)
    @Getter private SSCCredentials credentials;
	
	@Mixin
	@Getter private LoginSessionAliasMixin alias;

    static class SSCCredentials {
    	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC User credentials:%n", order = 3) @Getter private LoginUserCredentialOptions user;
    	@ArgGroup(exclusive = false, multiplicity = "1", heading = "SSC Token credentials:%n", order = 4) @Getter private TokenCredentials token;
    }
    
    static class TokenCredentials {
    	@Option(names = {"--token", "-t"}, required = true, interactive = true, arity = "0..1", echo = false) 
    	@Getter private char[] token;
    }
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
