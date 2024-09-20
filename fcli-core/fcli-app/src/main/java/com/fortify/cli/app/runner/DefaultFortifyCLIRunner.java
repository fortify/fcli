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
package com.fortify.cli.app.runner;

import java.util.Arrays;
import java.util.List;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;
import com.fortify.cli.app.runner.util.FortifyCLIDynamicInitializer;
import com.fortify.cli.app.runner.util.FortifyCLIStaticInitializer;
import com.fortify.cli.common.cli.util.FortifyCLIDefaultValueProvider;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.variable.FcliVariableHelper;

import picocli.CommandLine;

public final class DefaultFortifyCLIRunner implements IFortifyCLIRunner {
    // TODO See https://github.com/remkop/picocli/issues/2066
    //@Getter(value = AccessLevel.PRIVATE, lazy = true)
	//private final CommandLine commandLine = createCommandLine();
	
	private CommandLine createCommandLine() {
	    FortifyCLIStaticInitializer.getInstance().initialize();
	    CommandLine cl = new CommandLine(FCLIRootCommands.class);
	    // Custom parameter exception handler is disabled for now as it causes https://github.com/fortify/fcli/issues/434.
	    // See comments in I18nParameterExceptionHandler for more detail.
	    //cl.setParameterExceptionHandler(new I18nParameterExceptionHandler(cl.getParameterExceptionHandler()));
	    cl.setDefaultValueProvider(FortifyCLIDefaultValueProvider.getInstance());
	    return cl;
    }
	
	@Override
	public int run(String... args) {
	    try {
	        // If first arg is 'fcli', remove it. This allows for passing 'fcli' command name
	        // to scratch Docker image, for consistency with non-scratch/shell-based images.
	        if ( args.length>0 && "fcli".equalsIgnoreCase(args[0]) ) {
	            args = Arrays.copyOfRange(args, 1, args.length);
	        }
    	    String[] resolvedArgs = FcliVariableHelper.resolveVariables(args);
    	    FortifyCLIDynamicInitializer.getInstance().initialize(resolvedArgs);
    	    //CommandLine cl = getCommandLine(); // TODO See https://github.com/remkop/picocli/issues/2066
    	    CommandLine cl = createCommandLine();
    	    cl.clearExecutionResults();
    	    return cl.execute(resolvedArgs);
	    } finally {
	        // TODO For now, this is required to ensure new connections are used for 
	        // every fcli invocation, as otherwise we may be using older proxy settings
	        // after proxy has been reconfigured.
	        GenericUnirestFactory.shutdown();
	    }
	}
	
	@Override
	public int run(List<String> args) {
	    return run(args.toArray(new String[] {}));
	}
	
	@Override
	public void close() {
	    GenericUnirestFactory.shutdown();
	}
}
