/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.app.runner;

import java.util.Arrays;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;
import com.fortify.cli.app.runner.util.FortifyCLIDefaultValueProvider;
import com.fortify.cli.app.runner.util.FortifyCLIDynamicInitializer;
import com.fortify.cli.app.runner.util.FortifyCLIStaticInitializer;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.cli.util.FcliExecutionStrategyFactory;
import com.fortify.cli.common.exception.FcliExecutionExceptionHandler;
import com.fortify.cli.common.variable.FcliVariableHelper;

import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi.Text;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.CommandSpec;

public final class DefaultFortifyCLIRunner {
    // TODO See https://github.com/remkop/picocli/issues/2066
    //@Getter(value = AccessLevel.PRIVATE, lazy = true)
    //private final CommandLine commandLine = createCommandLine();
    
    private static final CommandLine createCommandLine() {
        FortifyCLIStaticInitializer.getInstance().initialize();
        CommandLine cl = new CommandLine(FCLIRootCommands.class);
        FcliCommandSpecHelper.setRootCommandLine(cl);
        // Custom parameter exception handler is disabled for now as it causes https://github.com/fortify/fcli/issues/434.
        // See comments in I18nParameterExceptionHandler for more detail.
        //cl.setParameterExceptionHandler(new I18nParameterExceptionHandler(cl.getParameterExceptionHandler()));
        cl.setExecutionExceptionHandler(FcliExecutionExceptionHandler.INSTANCE);
        cl.setDefaultValueProvider(FortifyCLIDefaultValueProvider.getInstance());
        cl.setHelpFactory((commandSpec, colorScheme)->new FcliHelp(commandSpec, colorScheme));
        return cl;
    }
    
    public static final int run(String... args) {
        // If first arg is 'fcli', remove it. This allows for passing 'fcli' command name
        // to scratch Docker image, for consistency with non-scratch/shell-based images.
        if ( args.length>0 && "fcli".equalsIgnoreCase(args[0]) ) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        String[] resolvedArgs = FcliVariableHelper.resolveVariables(args);
        FortifyCLIDynamicInitializer.getInstance().initialize(resolvedArgs);
        //CommandLine cl = getCommandLine(); // TODO See https://github.com/remkop/picocli/issues/2066
        CommandLine cl = createCommandLine();
        FcliExecutionStrategyFactory.configureCommandLine(cl);
        cl.clearExecutionResults();
        return cl.execute(resolvedArgs);
    }
    
    private static final class FcliHelp extends CommandLine.Help {
        public FcliHelp(CommandSpec commandSpec, ColorScheme colorScheme) {
            super(commandSpec, colorScheme);
        }

        public FcliHelp(Object command, Ansi ansi) {
            super(command, ansi);
        }

        public FcliHelp(Object command) {
            super(command);
        }
        
        protected String makeSynopsisFromParts(int synopsisHeadingLength, Text optionText, Text groupsText, Text endOfOptionsText, Text positionalParamText, Text commandText) {
            boolean positionalsOnly = true;
            for (ArgGroupSpec group : commandSpec().argGroups()) {
                if (group.validate()) { // non-validating groups are not shown in the synopsis
                    positionalsOnly &= group.allOptionsNested().isEmpty();
                }
            }
            Text text;
            if (positionalsOnly) { // show end-of-options delimiter before the (all-positional params) groups
                text = positionalParamText.concat(optionText).concat(endOfOptionsText).concat(groupsText).concat(commandText);
            } else {
                text = positionalParamText.concat(optionText).concat(groupsText).concat(endOfOptionsText).concat(commandText);
            }
            return insertSynopsisCommandName(synopsisHeadingLength, text);
        }
    }
}