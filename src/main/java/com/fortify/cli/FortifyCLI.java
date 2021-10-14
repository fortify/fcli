package com.fortify.cli;

import java.util.Collection;

import com.fortify.cli.command.RootCommand;
import com.fortify.cli.command.util.ISubcommandsProvider;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import picocli.CommandLine;

public class FortifyCLI {
    private static int execute(Class<?> clazz, String[] args) {
        try (ApplicationContext context = ApplicationContext.builder(FortifyCLI.class, Environment.CLI).start()) {
        	RootCommand rootCommand = context.getBean(RootCommand.class); 
            MicronautFactory factory = new MicronautFactory(context);
			CommandLine commandLine = new CommandLine(rootCommand, factory). 
                 setCaseInsensitiveEnumValuesAllowed(true). 
                 setUsageHelpAutoWidth(true);
            addSubcommands(factory, commandLine, rootCommand);
            return commandLine.execute(args); 
        }
    }

    private static final void addSubcommands(MicronautFactory factory, CommandLine commandLine, Object command) {
		if ( command instanceof ISubcommandsProvider ) {
			Collection<?> subcommands = ((ISubcommandsProvider)command).getSubcommands();
			for ( Object subcommand : subcommands ) {
				CommandLine subCommandLine = new CommandLine(subcommand, factory);
				addSubcommands(factory, subCommandLine, subcommand);
				commandLine.addSubcommand(subCommandLine);
			}
		}
	}

	public static void main(String[] args) {
        int exitCode = execute(RootCommand.class, args);
        System.exit(exitCode); 
    }

    
}
