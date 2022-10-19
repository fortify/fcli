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
package com.fortify.cli.app;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.jasypt.normalization.Normalizer;

import com.fortify.cli.app.i18n.I18nParameterExceptionHandler;
import com.fortify.cli.common.variable.FcliVariableHelper;
import com.fortify.cli.config.language.manager.LanguageConfigManager;
import com.oracle.svm.core.annotate.AutomaticFeature;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import picocli.CommandLine;

/**
 * <p>This class provides the {@link #main(String[])} entrypoint into the application. 
 * It first configures logging and then loads the {@link PicocliRunner} class to
 * actually execute commands based on provided command line arguments.</p>
 * 
 * <p>This class is also responsible for registering some GraalVM features, allowing
 * the application to run properly as GraalVM native images.</p>
 * 
 * @author Ruud Senden
 */
public class FortifyCLI {

    /**
     * This is the main entry point for executing the Fortify CLI.
     * @param args Command line options passed to Fortify CLI
     */
    public static void main(String[] args) {
        System.exit(execute(args));
    }

    /**
     * This method starts the Micronaut {@link ApplicationContext}, then invokes all beans that implement the
     * {@link IFortifyCLIInitializer} interface prior to executing {@link CommandLine#execute(String...)}.
     * @param args Command line options passed to Fortify CLI
     * @return exit code
     */
    private static int execute(String[] args) {
        String[] resolvedArgs = FcliVariableHelper.resolveVariables(args);
        try (ApplicationContext applicationContext = ApplicationContext.builder(FortifyCLI.class, Environment.CLI).start()) {
            try ( MicronautFactory micronautFactory = new MicronautFactory(applicationContext) ) {
                applicationContext.getBeansOfType(IFortifyCLIInitializer.class).forEach(b -> b.initializeFortifyCLI(resolvedArgs));
                CommandLine commandLine = new CommandLine(FCLIRootCommands.class, micronautFactory);
                return commandLine.setParameterExceptionHandler(
                            new I18nParameterExceptionHandler(
                                    commandLine.getParameterExceptionHandler(),
                                    applicationContext.getBean(LanguageConfigManager.class)
                            )
                ).execute(resolvedArgs);
            }
        }
    }
    
    /**
     * Register classes for runtime reflection in GraalVM native images
     */
    @AutomaticFeature
    public static final class RuntimeReflectionRegistrationFeature implements Feature {
        public void beforeAnalysis(BeforeAnalysisAccess access) {
            // This jasypt class uses reflection, so we perform a dummy operation to have GraalVM native image generation detect this
            Normalizer.normalizeToNfc("dummy");
            
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
