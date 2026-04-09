/*
 * Copyright 2021-2026 Open Text.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;
import com.fortify.cli.app.runner.util.FortifyCLIDefaultValueProvider;
import com.fortify.cli.app.runner.util.FortifyCLIDynamicInitializer;
import com.fortify.cli.app.runner.util.FortifyCLIStaticInitializer;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.cli.util.FcliExecutionStrategyFactory;
import com.fortify.cli.common.cli.util.FcliWrappedHelpExclude;
import com.fortify.cli.common.exception.FcliExecutionExceptionHandler;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.util.FcliDockerHelper;
import com.fortify.cli.common.variable.FcliVariableHelper;

import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Help.Ansi.Text;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

public final class DefaultFortifyCLIRunner {
    public static Ansi ANSI = Help.Ansi.AUTO;
    // TODO See https://github.com/remkop/picocli/issues/2066
    //@Getter(value = AccessLevel.PRIVATE, lazy = true)
    //private final CommandLine commandLine = createCommandLine();
    
    private static final CommandLine createCommandLine(boolean useWrapperHelp) {
        FortifyCLIStaticInitializer.getInstance().initialize();
        CommandLine cl = new CommandLine(FCLIRootCommands.class);
        cl.setColorScheme(Help.defaultColorScheme(ANSI));
        FcliCommandSpecHelper.setRootCommandLine(cl);
        // Custom parameter exception handler is disabled for now as it causes https://github.com/fortify/fcli/issues/434.
        // See comments in I18nParameterExceptionHandler for more detail.
        //cl.setParameterExceptionHandler(new I18nParameterExceptionHandler(cl.getParameterExceptionHandler()));
        cl.setExecutionExceptionHandler(FcliExecutionExceptionHandler.INSTANCE);
        cl.setDefaultValueProvider(FortifyCLIDefaultValueProvider.getInstance());
        cl.setHelpFactory((commandSpec, colorScheme)->useWrapperHelp 
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
        
        // Check for -Xwrapped option and remove it from args
        boolean isWrapped = Arrays.stream(args).anyMatch("-Xwrapped"::equals);
        if ( isWrapped ) {
            args = Arrays.stream(args).filter(arg -> !"-Xwrapped".equals(arg)).toArray(String[]::new);
        }
        
        // Replace --fcli-help with --help for wrapper compatibility
        args = Arrays.stream(args)
                .map(arg -> "--fcli-help".equals(arg) ? "--help" : arg)
                .toArray(String[]::new);
        
        String[] resolvedArgs = FcliVariableHelper.resolveVariables(args);
        FortifyCLIDynamicInitializer.getInstance().initialize(resolvedArgs);
        //CommandLine cl = getCommandLine(); // TODO See https://github.com/remkop/picocli/issues/2066
        CommandLine cl = createCommandLine(isWrapped);
        FcliExecutionStrategyFactory.configureCommandLine(cl);
        cl.clearExecutionResults();
        return cl.execute(resolvedArgs);
    }
    
    private static abstract class AbstractFcliHelp extends CommandLine.Help {
        public AbstractFcliHelp(CommandSpec commandSpec, ColorScheme colorScheme) {
            super(commandSpec, colorScheme);
            customizeHelpSections();
        }

        public AbstractFcliHelp(Object command, Ansi ansi) {
            super(command, ansi);
            customizeHelpSections();
        }

        public AbstractFcliHelp(Object command) {
            super(command);
            customizeHelpSections();
        }
        
        private void customizeHelpSections() {
            // Show 'Command options' heading only if there are ungrouped options
            commandSpec().usageMessage().sectionMap().put("optionListHeading", 
                help -> hasUngroupedOptions() ? createHeading(new CommandSpecMessageResolver(commandSpec()).getMessageString("usage.commandOptionsListHeading")) : "");
        }
        
        private boolean hasUngroupedOptions() {
            List<OptionSpec> visibleOptionsNotInGroups = new ArrayList<>(commandSpec().options());
            for (ArgGroupSpec group : optionSectionGroups()) {
                visibleOptionsNotInGroups.removeAll(group.allOptionsNested());
            }
            visibleOptionsNotInGroups.removeIf(OptionSpec::hidden);
            return !visibleOptionsNotInGroups.isEmpty();
        }
        
        @Override
        public String description(Object... params) {
            return super.description(params).trim();
        }
        
        @Override
        public String descriptionHeading(Object... params) {
            // Suppress description heading to avoid extra blank lines when description
            // is empty (description is shown at top before synopsis)
            return "";
        }
        
        @Override
        public String footer(Object... params) {
            int width = commandSpec().usageMessage().width();
            String dockerNotice = FcliDockerHelper.getDockerHelpNotice(width);
            String[] footerStrings = commandSpec().usageMessage().footer();
            String[] combinedFooters = dockerNotice.isBlank() 
                ? footerStrings 
                : Stream.concat(Stream.of(dockerNotice), Arrays.stream(footerStrings)).toArray(String[]::new);
            return join(ansi(), width, 
                commandSpec().usageMessage().adjustLineBreaksForWideCJKCharacters(), 
                combinedFooters, new StringBuilder(), params).toString();
        }
        
        @Override
        protected String makeSynopsisFromParts(int synopsisHeadingLength, Text optionText, Text groupsText, Text endOfOptionsText, Text positionalParamText, Text commandText) {
            boolean positionalsOnly = true;
            for (ArgGroupSpec group : commandSpec().argGroups()) {
                if (group.validate()) {
                    positionalsOnly &= group.allOptionsNested().isEmpty();
                }
            }
            Text text;
            if (positionalsOnly) {
                text = positionalParamText.concat(optionText).concat(endOfOptionsText).concat(groupsText).concat(commandText);
            } else {
                text = positionalParamText.concat(optionText).concat(groupsText).concat(endOfOptionsText).concat(commandText);
            }
            return insertSynopsisCommandName(synopsisHeadingLength, text);
        }
        
        @Override
        public String optionListGroupSections() {
            // Filter out groups marked with @FcliWrappedHelpExclude
            StringBuilder result = new StringBuilder();
            for (ArgGroupSpec group : optionSectionGroups()) {
                if (!isExcludedOptionsGroup(group)) {
                    result.append(createHeading(group.heading()));
                    result.append(renderGroupLayout(group));
                }
            }
            return result.toString();
        }
        
        protected boolean isExcludedOptionsGroup(ArgGroupSpec group) {
            return group.allOptionsNested().isEmpty();
        }
        
        private String renderGroupLayout(ArgGroupSpec group) {
            Layout layout = createDefaultLayout();
            layout.addOptions(new ArrayList<>(group.allOptionsNested()), parameterLabelRenderer());
            return layout.toString();
        }
    }
    
    private static final class FcliHelp extends AbstractFcliHelp {
        public FcliHelp(CommandSpec commandSpec, ColorScheme colorScheme) {
            super(commandSpec, colorScheme);
        }

        public FcliHelp(Object command, Ansi ansi) {
            super(command, ansi);
        }

        public FcliHelp(Object command) {
            super(command);
        }
    }
    
    /**
     * Custom Help class for wrapper tools (when -Xwrapped is used).
     * Suppresses synopsis, footer, and generic fcli options sections to show 
     * only usage header, description, and command-specific option descriptions.
     */
    private static final class FcliWrapperHelp extends AbstractFcliHelp {
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
            return "";
        }
        
        @Override
        public String synopsis(int synopsisHeadingLength) {
            return "";
        }
        
        @Override
        public String footerHeading(Object... params) {
            return "";
        }
        
        @Override
        public String footer(Object... params) {
            return "";
        }
        
        @Override
        protected boolean isExcludedOptionsGroup(ArgGroupSpec group) {
            return super.isExcludedOptionsGroup(group) || isWrappedHelpExclude(group);
        }
        
        private boolean isWrappedHelpExclude(ArgGroupSpec group) {
            try {
                Object userObject = group.getter().get();
                return userObject != null && userObject.getClass().isAnnotationPresent(FcliWrappedHelpExclude.class);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * @param group
     * @return
     */
    public boolean isExcludedOptionsGroup(ArgGroupSpec group) {
        // TODO Auto-generated method stub
        return false;
    }
}