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
package com.fortify.cli.common.picocli.executor;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;

import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

/**
 * This class is responsible for generating the subcommands hierarchy.
 * 
 * TODO Clean up this class
 * 
 * @author Ruud Senden
 */
@Singleton
public class SubcommandsHelper {
	private final LinkedHashMap<Class<?>, List<Object>> parentToSubcommandsMap = new LinkedHashMap<>();
	private final LinkedHashMap<Class<?>, Boolean> hasRunnableSubcommands = new LinkedHashMap<>();
	private final ApplicationContext applicationContext;
	private final EnabledCommandBeansHelper enabledCommandBeansHelper;
	private final MicronautFactorySupplier micronautFactorySupplier;
	
	@Inject
	public SubcommandsHelper(ApplicationContext applicationContext,
			EnabledCommandBeansHelper enabledCommandBeansHelper,
			MicronautFactorySupplier micronautFactorySupplier) {
		this.applicationContext = applicationContext;
		this.enabledCommandBeansHelper = enabledCommandBeansHelper;
		this.micronautFactorySupplier = micronautFactorySupplier;
	}
	
	/**
	 * Add the full hierarchy of sub-commands to the given {@link CommandLine} instance.
	 * @param rootCommandLine to which to add the full sub-command hierarchy
	 */
	public final void addSubcommands(CommandLine rootCommandLine) {
		addSubcommands(rootCommandLine, rootCommandLine.getCommand());
	}
	
	/**
	 * Add the partial sub-command hierarchy for the given command to the given
	 * {@link CommandLine} instance.
	 * 
	 * @param commandLine to which to add the partial sub-command hierarchy
	 * @param command for which to generate the sub-command hierarchy
	 */
	private final void addSubcommands(CommandLine commandLine, Object command) {
		List<Object> subcommands = parentToSubcommandsMap.get(command.getClass());
		if (subcommands != null) {
			for (Object subcommand : subcommands) {
				addSubcommand(commandLine, subcommand);
			}
		}
	}

	/**
	 * Add a single sub-command tree for the given command to the given {@link CommandLine} instance.
	 * If the command has no runnable sub-commands, it will be replaced by a {@link DisabledCommand}
	 * instance.
	 * @param commandLine TODO
	 * @param subcommand TODO
	 */
	private void addSubcommand(CommandLine commandLine, Object subcommand) {
		CommandLine subCommandLine = new CommandLine(subcommand, micronautFactorySupplier.getMicronautFactory());
		boolean isSubCommandEnabled = Boolean.TRUE.equals(hasRunnableSubcommands.get(subcommand.getClass())) || enabledCommandBeansHelper.isAlphaFeaturesEnabled();
		if ( !isSubCommandEnabled ) {
			String name = subCommandLine.getCommandSpec().name();
			String[] aliases = subCommandLine.getCommandSpec().aliases();
			String reason = "No runnable sub-commands";
			DisabledCommand disabledCommand = new DisabledCommand(name, aliases, reason);
			subCommandLine = new CommandLine(disabledCommand.asCommandSpec());
		}
		try {
			commandLine.addSubcommand(subCommandLine);
		} catch ( RuntimeException e ) {
			throw new RuntimeException("Error adding command class "+subcommand.getClass().getName(), e);
		}
		if ( isSubCommandEnabled ) {
			addSubcommands(subCommandLine, subcommand);
		}
	}
	
	@PostConstruct
	private final void build() {
		applicationContext.getBeanDefinitions(Qualifiers.byStereotype(SubcommandOf.class))
			.stream()
			.sorted(CommandBeanComparator.INSTANCE)
			.forEach(this::addSubcommand);
	}
	
	private final void addSubcommand(BeanDefinition<?> commandBeanDefinition) {
		Object command = getCommand(commandBeanDefinition);
		addMultiValueEntry(
				parentToSubcommandsMap, 
				getParentCommandClazz(commandBeanDefinition),
				command);
		if ( command instanceof Runnable || command instanceof Callable ) {
			setParentCommandsEnabled(command.getClass());
		}
	}
	
	private void setParentCommandsEnabled(Class<?> clazz) {
		hasRunnableSubcommands.put(clazz, true);
		SubcommandOf subcommandOf = clazz.getAnnotation(SubcommandOf.class);
		if ( subcommandOf!=null ) {
			Class<?> parentClazz = subcommandOf.value();
			setParentCommandsEnabled(parentClazz);
		}
	}

	private Object getCommand(BeanDefinition<?> commandBeanDefinition) {
		if ( enabledCommandBeansHelper.isEnabled(commandBeanDefinition) ) {
			return applicationContext.getBean(commandBeanDefinition);
		} else {
			String name = commandBeanDefinition.getAnnotation(Command.class).stringValue("name").orElseThrow();
			String[] aliases = commandBeanDefinition.getAnnotation(Command.class).stringValues("aliases");
			String reason = enabledCommandBeansHelper.getDisabledReason(commandBeanDefinition);
			return new DisabledCommand(name, aliases, reason).asCommandSpec();
		}
	}
	
	private static final Class<?> getParentCommandClazz(BeanDefinition<?> bd) {
		Optional<Class<?>> optClazz = bd.getAnnotation(SubcommandOf.class).classValue();
		if ( !optClazz.isPresent() ) {
			throw new IllegalStateException("No parent command found for class "+bd.getBeanType().getName());
		}
		return optClazz.get();
	}

	private static final <K, V> void addMultiValueEntry(LinkedHashMap<K, List<V>> map, K key, V value) {
		map.computeIfAbsent(key, k->new LinkedList<V>()).add(value);
	}
	
	@RequiredArgsConstructor @Command(hidden = true, description = {"This command is currently disabled"})
	public static final class DisabledCommand implements Runnable {
		private final String name;
		private final String[] aliases;
		private final String reason;
		
		public final CommandSpec asCommandSpec() {
			return new CommandLine(this)
					.setUnmatchedArgumentsAllowed(true)
					.setUnmatchedOptionsArePositionalParams(true)
					.getCommandSpec()
						.name(name)
						.aliases(aliases);
		}
		
		@Override
		public void run() {
			throw new IllegalStateException(String.format("Command '%s' is disabled: %s", name, reason));
		}
	}
}
