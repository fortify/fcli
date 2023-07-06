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

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;
import com.fortify.cli.app.i18n.I18nParameterExceptionHandler;
import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.variable.FcliVariableHelper;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import picocli.CommandLine;

public final class DefaultFortifyCLIRunner implements IFortifyCLIRunner {
    private boolean initialized = false;
	private ApplicationContext applicationContext;
	private MicronautFactory micronautFactory;
	private CommandLine commandLine;
	
	private synchronized void initialize(String[] args) {
	    if ( !initialized ) {
	        this.applicationContext = ApplicationContext.builder(DefaultFortifyCLIRunner.class, Environment.CLI).start();
	        this.micronautFactory = new MicronautFactory(applicationContext);
	        FortifyCLIInitializerRunner.initialize(args, micronautFactory);
	        this.commandLine = new CommandLine(FCLIRootCommands.class, micronautFactory);
	        this.commandLine = commandLine.setParameterExceptionHandler(new I18nParameterExceptionHandler(commandLine.getParameterExceptionHandler()));
	        this.initialized = true;
	    }
    }
	
	@Override
	public int run(String... args) {
	    String[] resolvedArgs = FcliVariableHelper.resolveVariables(args);
	    // TODO We only initialize once even though args may vary if this instance
	    //      is used for multiple fcli invocations. Check whether we can use
	    //      https://picocli.info/#_initialization_before_execution instead.
	    //      To optimize, potentially we can have two types of initializers;
	    //      static initializers that don't depend on args, and execution strategy
	    //      based initializers that do per-command initialization. Note that those
	    //      dynamic initializers preferably should be thread-safe, to allow for
	    //      running multi-threaded functional tests. 
	    initialize(args);
	    commandLine.clearExecutionResults();
	    return commandLine.execute(resolvedArgs);
	}
	
	@Override
	public void close() {
	    if ( this.initialized ) {
	        GenericUnirestFactory.shutdown();
	        micronautFactory.close();
	        applicationContext.close();
	    }
	}
}
