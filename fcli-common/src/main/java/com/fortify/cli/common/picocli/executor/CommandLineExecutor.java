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
package com.fortify.cli.common.picocli.executor;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.FCLIRootCommand;
import com.fortify.cli.common.picocli.util.DefaultValueProvider;

import io.micronaut.context.annotation.Executable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

/**
 * This class is responsible for actually running the (sub-)commands 
 * specified on the command line by generating a {@link CommandLine}
 * instance containing an {@link FCLIRootCommand} instance and the 
 * full sub-command hierarchy underneath that, based on the {@link SubcommandOf}
 * annotation. Once the {@link CommandLine} has been constructed,
 * it will be executed in order to have picocli invoke the appropriate
 * sub-command.
 * 
 * @author Ruud Senden
 */
@Singleton
public class CommandLineExecutor {
	private final FCLIRootCommand rootCommand;
	private final SubcommandsHelper subcommandsHelper;
	private final MicronautFactorySupplier micronautFactorySupplier;
	private CommandLine commandLine;
	@Inject
	public CommandLineExecutor(
			FCLIRootCommand rootCommand,  
			MicronautFactorySupplier micronautFactorySupplier,
			SubcommandsHelper subcommandsHelper) {
		this.rootCommand = rootCommand;
		this.subcommandsHelper = subcommandsHelper;
		this.micronautFactorySupplier = micronautFactorySupplier;
	}
	
	@PostConstruct @Executable
	public void createCommandLine() {
		this.commandLine = new CommandLine(rootCommand, micronautFactorySupplier.getMicronautFactory())
				.setCaseInsensitiveEnumValuesAllowed(true)
				.setDefaultValueProvider(new DefaultValueProvider())
				.setUsageHelpAutoWidth(true); // TODO Add ExceptionHandler?
		subcommandsHelper.addSubcommands(commandLine);
	}
	
	public final int execute(String[] args) {
		return commandLine.execute(args);
	}
}
