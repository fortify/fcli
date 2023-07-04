/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.graalvm.nativeimage.hosted.Feature;
import org.jasypt.normalization.Normalizer;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.app.runner.DefaultFortifyCLIRunner;
import com.oracle.svm.core.annotate.AutomaticFeature;

import io.micronaut.core.annotation.TypeHint;

/**
 * <p>This class provides the {@link #main(String[])} entrypoint into the application,
 * and also registers some GraalVM features, allowing the application to run properly 
 * as GraalVM native images.</p>
 * 
 * @author Ruud Senden
 */
public class FortifyCLI {
	private static final Boolean JANSI_DISABLE = Boolean.getBoolean("jansi.disable");

    /**
     * This is the main entry point for executing the Fortify CLI.
     * @param args Command line options passed to Fortify CLI
     */
    public static final void main(String[] args) {
        System.exit(execute(args));
    }

    private static final int execute(String[] args) {
        try ( var runner = new DefaultFortifyCLIRunner() ) {
            installAnsiConsole();
            return runner.run(args);
        } finally {
            uninstallAnsiConsole();
        }
    }
    
    private static final void installAnsiConsole() {
    	tryInvokeAnsiConsoleMethod("systemInstall");
    }
    
    private static final void uninstallAnsiConsole() {
    	tryInvokeAnsiConsoleMethod("systemUninstall");
    }
    
    private static final void tryInvokeAnsiConsoleMethod(String methodName) {
    	if ( !JANSI_DISABLE ) {
	    	try {
	    		// AnsiConsole performs eager initialization in a static block, so
	    		// referencing the class directly would initialize Jansi even if
	    		// isJansiEnabled() returns false. As such, we use reflection to 
	    		// only load the AnsiConsole class if Jansi is enabled, and then
	    		// invoke the specified method. Note that in order for this to work, 
	    		// we have a reflect-config.json file to allow reflective access to
	    		// AnsiConsole.
	    		Class.forName("org.fusesource.jansi.AnsiConsole")
	    			.getMethod(methodName).invoke(null);
	    	} catch ( Throwable t ) {
	    		t.printStackTrace();
	    	}
    	}
    }
    
/**
 * Register classes for runtime reflection in GraalVM native images. The
 * {@link TypeHint} annotation is used to generate reflect-config.json 
 * for some standard Java data classes on which we may want to reflectively 
 * invoke methods from SpEL expressions. Alternatively, we could look into 
 * creating a custom MethodResolver that covers all of these classes and 
 * potentially doesn't require reflective access.
 */
 @TypeHint(
         value = {
             Boolean.class,
             Double.class,
             Float.class,
             Integer.class,
             Long.class,
             Short.class,
             String.class,
             ObjectNode.class,
             ArrayNode.class,
             ArrayList.class,
             LinkedHashMap.class,
             HashSet.class
         },
         accessType = TypeHint.AccessType.ALL_PUBLIC_METHODS
     )
    @AutomaticFeature
    public static final class RuntimeReflectionRegistrationFeature implements Feature {
        public void beforeAnalysis(BeforeAnalysisAccess access) {
            // This jasypt class uses reflection, so we perform a dummy operation to have GraalVM native image generation detect this
            Normalizer.normalizeToNfc("dummy");
        }
    }
}
