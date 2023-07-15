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

import java.util.List;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;
import com.fortify.cli.app.runner.util.FortifyCLIDefaultValueProvider;
import com.fortify.cli.app.runner.util.FortifyCLIDynamicInitializer;
import com.fortify.cli.app.runner.util.FortifyCLIStaticInitializer;
import com.fortify.cli.app.runner.util.I18nParameterExceptionHandler;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.variable.FcliVariableHelper;

import lombok.AccessLevel;
import lombok.Getter;
import picocli.CommandLine;

public final class DefaultFortifyCLIRunner implements IFortifyCLIRunner {
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
	private final CommandLine commandLine = createCommandLine();
	
	private CommandLine createCommandLine() {
	    FortifyCLIStaticInitializer.getInstance().initialize();
	    CommandLine cl = new CommandLine(FCLIRootCommands.class);
	    cl.setParameterExceptionHandler(new I18nParameterExceptionHandler(cl.getParameterExceptionHandler()));
	    cl.setDefaultValueProvider(FortifyCLIDefaultValueProvider.getInstance());
	    return cl;
    }
	
	@Override
	public int run(String... args) {
	    String[] resolvedArgs = FcliVariableHelper.resolveVariables(args);
	    FortifyCLIDynamicInitializer.getInstance().initialize(resolvedArgs);
	    CommandLine cl = getCommandLine();
	    cl.clearExecutionResults();
	    return cl.execute(resolvedArgs);
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
