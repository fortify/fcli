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
package com.fortify.cli.common.cli.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(defaultValueProvider = FortifyCLIDefaultValueProvider.class)
public class FortifyCLIInitializerRunner {
    private static final PrintWriter DUMMY_WRITER = new PrintWriter(new StringWriter());
    
    public static final void initialize(String[] args) {
        // Remove help options, as we want initialization always to occur
        String[] argsWithoutHelp = Stream.of(args).filter(a->!a.matches("-h|--help")).toArray(String[]::new);
        new CommandLine(FortifyCLIInitializerCommand.class)
                .setOut(DUMMY_WRITER)
                .setErr(DUMMY_WRITER)
                .setUnmatchedArgumentsAllowed(true)
                .setUnmatchedOptionsArePositionalParams(true)
                .setExpandAtFiles(true)
                .execute(argsWithoutHelp);
    }
    
    @Command(name = "fcli", defaultValueProvider = FortifyCLIDefaultValueProvider.class)
    public static final class FortifyCLIInitializerCommand extends AbstractFortifyCLICommand implements Runnable {
        private IFortifyCLIInitializer[] initializers = {};
        @Getter @Spec private CommandSpec commandSpec;
        
        @Override
        public void run() {
            for ( var initializer : initializers ) {
                initializer.initializeFortifyCLI(this);
            }
        }
    }
}
