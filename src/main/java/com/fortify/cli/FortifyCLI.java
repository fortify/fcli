package com.fortify.cli;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.fortify.cli.command.RootCommand;
import com.fortify.cli.command.util.DefaultValueProvider;
import com.fortify.cli.command.util.SubcommandOf;
import com.oracle.svm.core.annotate.AutomaticFeature;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;
import picocli.CommandLine;

public class FortifyCLI {
	private static int execute(Class<?> clazz, String[] args) {
		try (ApplicationContext context = ApplicationContext.builder(FortifyCLI.class, Environment.CLI).start()) {
			RootCommand rootCommand = context.getBean(RootCommand.class);
			MicronautFactory factory = new MicronautFactory(context);
			CommandLine commandLine = new CommandLine(rootCommand, factory)
					.setCaseInsensitiveEnumValuesAllowed(true)
					.setDefaultValueProvider(new DefaultValueProvider())
					.setUsageHelpAutoWidth(true);
			addSubcommands(context, factory, commandLine, rootCommand);
			return commandLine.execute(args);
		}
	}

	private static void addSubcommands(ApplicationContext context, MicronautFactory factory, CommandLine commandLine,
			RootCommand rootCommand) {
		Map<Class<?>, List<Object>> parentToSubcommandsMap = getParentToSubcommandsMap(context);
		addSubcommands(parentToSubcommandsMap, factory, commandLine, rootCommand);
	}

	private static final void addSubcommands(Map<Class<?>, List<Object>> parentToSubcommandsMap,
			MicronautFactory factory, CommandLine commandLine, Object command) {
		List<Object> subcommands = parentToSubcommandsMap.get(command.getClass());
		if (subcommands != null) {
			for (Object subcommand : subcommands) {
				CommandLine subCommandLine = new CommandLine(subcommand, factory);
				commandLine.addSubcommand(subCommandLine);
				addSubcommands(parentToSubcommandsMap, factory, subCommandLine, subcommand);
			}
		}
	}

	private static final Map<Class<?>, List<Object>> getParentToSubcommandsMap(ApplicationContext context) {
		final var beanDefinitions = context.getBeanDefinitions(Qualifiers.byStereotype(SubcommandOf.class));
		
		/* Disabled for now as for some reason compilation intermittently fails on this statement
		return beanDefinitions.stream().collect(
			Collectors.groupingBy(bd -> bd.getAnnotation(SubcommandOf.class).classValue().get(),
			                      Collectors.mapping(context::getBean, Collectors.toList())));
		*/
		var parentToSubcommandsMap = new LinkedHashMap<Class<?>, List<Object>>();
		beanDefinitions.stream().sorted(FortifyCLI::compare).forEach(bd ->
			addMultiValueEntry(
				parentToSubcommandsMap, 
				bd.getAnnotation(SubcommandOf.class).classValue().get(),
				context.getBean(bd)));
		return parentToSubcommandsMap;
	}
	
	private static int compare(BeanDefinition<?> bd1, BeanDefinition<?> bd2) {
		return Integer.compare(OrderUtil.getOrder(bd1.getAnnotationMetadata()), OrderUtil.getOrder(bd2.getAnnotationMetadata()));
	}

	private static <K, V> void addMultiValueEntry(LinkedHashMap<K, List<V>> map, K key, V value) {
		map.computeIfAbsent(key, k->new LinkedList<V>()).add(value);
	}

	public static void main(String[] args) {
		int exitCode = execute(RootCommand.class, args);
		System.exit(exitCode);
	}
	
	@AutomaticFeature
	public static final class RuntimeReflectionRegistrationFeature implements Feature {
		public void beforeAnalysis(BeforeAnalysisAccess access) {
			// TODO Review whether these are all necessary
			RuntimeReflection.register(String.class);
			RuntimeReflection.register(LogFactoryImpl.class);
			RuntimeReflection.register(LogFactoryImpl.class.getDeclaredConstructors());
			RuntimeReflection.register(LogFactory.class);
			RuntimeReflection.register(LogFactory.class.getDeclaredConstructors());
			RuntimeReflection.register(SimpleLog.class);
			RuntimeReflection.register(SimpleLog.class.getDeclaredConstructors());
		}
	}
}
