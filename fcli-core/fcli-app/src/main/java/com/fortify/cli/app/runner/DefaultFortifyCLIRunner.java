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
import com.fortify.cli.common.cli.util.FcliHelpExclude;
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
    
    private static final CommandLine createCommandLine(boolean isFcliHelp) {
        FortifyCLIStaticInitializer.getInstance().initialize();
        CommandLine cl = new CommandLine(FCLIRootCommands.class);
        FcliCommandSpecHelper.setRootCommandLine(cl);
        // Custom parameter exception handler is disabled for now as it causes https://github.com/fortify/fcli/issues/434.
        // See comments in I18nParameterExceptionHandler for more detail.
        //cl.setParameterExceptionHandler(new I18nParameterExceptionHandler(cl.getParameterExceptionHandler()));
        cl.setExecutionExceptionHandler(FcliExecutionExceptionHandler.INSTANCE);
        cl.setDefaultValueProvider(FortifyCLIDefaultValueProvider.getInstance());
        cl.setHelpFactory((commandSpec, colorScheme)->isFcliHelp 
                ? new FcliWrapperHelp(commandSpec, colorScheme) 
                : new FcliHelp(commandSpec, colorScheme));
        return cl;
    }
    
    public static final int run(String... args) {
        // If first arg is 'fcli', remove it. This allows for passing 'fcli' command name
        // to scratch Docker image, for consistency with non-scratch/shell-based images.
        if ( args.length>0 && "fcli".equalsIgnoreCase(args[0]) ) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        
        // Check for --fcli-help and replace with --help
        boolean isFcliHelp = false;
        for (int i = 0; i < args.length; i++) {
            if ("--fcli-help".equals(args[i])) {
                args[i] = "--help";
                isFcliHelp = true;
                break;
            }
        }
        
        String[] resolvedArgs = FcliVariableHelper.resolveVariables(args);
        FortifyCLIDynamicInitializer.getInstance().initialize(resolvedArgs);
        //CommandLine cl = getCommandLine(); // TODO See https://github.com/remkop/picocli/issues/2066
        CommandLine cl = createCommandLine(isFcliHelp);
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
    
    /**
     * Custom Help class for wrapper tools (when --fcli-help is used).
     * Suppresses synopsis, footer, and generic fcli options sections to show 
     * only usage header, description, and command-specific option descriptions.
     */
    private static final class FcliWrapperHelp extends CommandLine.Help {
        public FcliWrapperHelp(CommandSpec commandSpec, ColorScheme colorScheme) {
            super(commandSpec, colorScheme);
        }

        public FcliWrapperHelp(Object command, Ansi ansi) {
            super(command, ansi);
        }

        public FcliWrapperHelp(Object command) {
            super(command);
        }
        
        @Override
        public String synopsisHeading(Object... params) {
            return ""; // Suppress synopsis heading
        }
        
        @Override
        public String synopsis(int synopsisHeadingLength) {
            return ""; // Suppress synopsis
        }
        
        @Override
        public String footerHeading(Object... params) {
            return ""; // Suppress footer heading
        }
        
        @Override
        public String footer(Object... params) {
            return ""; // Suppress footer
        }
        
        @Override
        public String optionListGroupSections() {
            // Render all option groups except generic options
            StringBuilder result = new StringBuilder();
            for (ArgGroupSpec group : optionSectionGroups()) {
                if (!isGenericOptionsGroup(group)) {
                    result.append(createHeading(group.heading()));
                    result.append(renderGroupLayout(group));
                }
            }
            return result.toString();
        }
        
        private boolean isGenericOptionsGroup(ArgGroupSpec group) {
            try {
                Object userObject = group.getter().get();
                return userObject != null && userObject.getClass().isAnnotationPresent(FcliHelpExclude.class);
            } catch (Exception e) {
                return false;
            }
        }
        
        private String renderGroupLayout(ArgGroupSpec group) {
            Layout layout = createDefaultLayout();
            layout.addOptions(new java.util.ArrayList<>(group.allOptionsNested()), parameterLabelRenderer());
            return layout.toString();
        }
    }
}