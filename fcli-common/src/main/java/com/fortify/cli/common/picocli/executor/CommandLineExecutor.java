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

import java.util.List;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.FCLIRootCommand;
import com.fortify.cli.common.picocli.util.DefaultValueProvider;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
	private final SubcommandsMap subcommandsMap;
	private final MicronautFactory factory;
	private CommandLine commandLine;
	@Inject
	public CommandLineExecutor(
			FCLIRootCommand rootCommand,  
			ApplicationContext applicationContext,
			SubcommandsMap subcommandsMap) {
		this.rootCommand = rootCommand;
		this.subcommandsMap = subcommandsMap;
		this.factory = new MicronautFactory(applicationContext);
	}
	
	@PreDestroy
	public void closeFactory() {
		this.factory.close();
	}
	
	@PostConstruct
	public void createCommandLine() {
		this.commandLine = new CommandLine(rootCommand, factory)
				.setCaseInsensitiveEnumValuesAllowed(true)
				.setDefaultValueProvider(new DefaultValueProvider())
				.setUsageHelpAutoWidth(true); // TODO Add ExceptionHandler?
		addSubcommands(commandLine, rootCommand);
	}
	
	public final int execute(String[] args) {
		return commandLine.execute(args);
	}
	
	private final void addSubcommands(CommandLine commandLine, Object command) {
		List<Object> subcommands = subcommandsMap.get(command.getClass());
		if (subcommands != null) {
			for (Object subcommand : subcommands) {
				CommandLine subCommandLine = new CommandLine(subcommand, factory);
				try {
					commandLine.addSubcommand(subCommandLine);
				} catch ( RuntimeException e ) {
					throw new RuntimeException("Error while adding command class "+subcommand.getClass().getName(), e);
				}
				addSubcommands(subCommandLine, subcommand);
			}
		}
	}
}
