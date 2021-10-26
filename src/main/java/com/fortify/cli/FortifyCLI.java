package com.fortify.cli;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.fortify.cli.common.command.CommandLineExecutor;
import com.oracle.svm.core.annotate.AutomaticFeature;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;

public class FortifyCLI {
	/**
	 * This is the main entry point for executing the Fortify CLI. It will configure logging and
	 * then get a {@link CommandLineExecutor} instance from Micronaut, which will perform the
	 * actual work in its {@link CommandLineExecutor#execute(String[])} method.
	 * @param args Command line options passed to Fortify CLI
	 */
	public static void main(String[] args) {
		FortifyCLILogHelper.configureLogging(args);
		System.exit(execute(args));
	}
	
	/**
	 * This method starts the Micronaut {@link ApplicationContext}, then invokes the 
	 * {@link CommandLineExecutor#execute(String[])} method on the {@link CommandLineExecutor}
	 * singleton retrieved from the Micronaut {@link ApplicationContext}
	 * @param args Command line options passed to Fortify CLI
	 * @return exit code
	 */
	private static int execute(String[] args) {
		try (ApplicationContext context = ApplicationContext.builder(FortifyCLI.class, Environment.CLI).start()) {
			CommandLineExecutor runner = context.getBean(CommandLineExecutor.class);
			return runner.execute(args);
		}
	}
	
	/**
	 * Register classes for runtime reflection in GraalVM native images
	 */
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
