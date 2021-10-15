package com.fortify.cli;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fortify.cli.command.RootCommand;
import com.fortify.cli.command.util.SubcommandOf;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;
import picocli.CommandLine;

public class FortifyCLI {
    private static int execute(Class<?> clazz, String[] args) {
        try (ApplicationContext context = ApplicationContext.builder(FortifyCLI.class, Environment.CLI).start()) {
        	RootCommand rootCommand = context.getBean(RootCommand.class); 
            MicronautFactory factory = new MicronautFactory(context);
			CommandLine commandLine = new CommandLine(rootCommand, factory). 
                 setCaseInsensitiveEnumValuesAllowed(true). 
                 setUsageHelpAutoWidth(true);
            addSubcommands(context, factory, commandLine, rootCommand);
            return commandLine.execute(args); 
        }
    }

    private static void addSubcommands(ApplicationContext context, MicronautFactory factory, CommandLine commandLine, RootCommand rootCommand) {
    	Map<Class<?>, List<Object>> parentToSubcommandsMap = getParentToSubcommandsMap(context);
		addSubcommands(parentToSubcommandsMap, factory, commandLine, rootCommand);
	}
    
    private static final void addSubcommands(Map<Class<?>, List<Object>> parentToSubcommandsMap, MicronautFactory factory, CommandLine commandLine, Object command) {
    	List<Object> subcommands = parentToSubcommandsMap.get(command.getClass());
    	if ( subcommands!=null ) {
			for ( Object subcommand : subcommands ) {
				CommandLine subCommandLine = new CommandLine(subcommand, factory);
				addSubcommands(parentToSubcommandsMap, factory, subCommandLine, subcommand);
				commandLine.addSubcommand(subCommandLine);
			}
		}
	}

	private static final Map<Class<?>, List<Object>> getParentToSubcommandsMap(ApplicationContext context) {
		// TODO: Use proper Qualifier to get only SubcommandOf-annotated beans instead of filtering manually
		Collection<BeanDefinition<?>> beanDefinitions = context.getBeanDefinitions(Qualifiers.any());
		return beanDefinitions.stream()
			.filter(bd->bd.hasAnnotation(SubcommandOf.class))
			.collect(Collectors.groupingBy(
				bd->bd.getAnnotation(SubcommandOf.class).classValue().get(), 
				Collectors.mapping(context::getBean, Collectors.toList())));
	}

	public static void main(String[] args) {
        int exitCode = execute(RootCommand.class, args);
        System.exit(exitCode); 
    }
}
