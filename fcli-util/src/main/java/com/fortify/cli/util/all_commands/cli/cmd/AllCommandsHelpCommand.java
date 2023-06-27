/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.all_commands.cli.cmd;

import java.util.LinkedHashSet;
import java.util.Map;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "help")
public final class AllCommandsHelpCommand extends AbstractFortifyCLICommand implements Runnable {
    @Spec private CommandSpec spec;
    @Option(names = "--include-hidden") private boolean includeHidden;
    @Option(names = "--include-parents") private boolean includeParents;
    
    @Override
    public final void run() {
        CommandSpec root = spec.root();
        printHelp(root);
        run(root.subcommands());
    }
    
    private final void run(Map<String, CommandLine> subcommands) {
        if ( subcommands!=null && !subcommands.isEmpty() ) {
            // We wrap subcommands.values() in a LinkedHashSet to remove duplicates
            // (due to command aliases) while keeping original order
            for (CommandLine cl : new LinkedHashSet<>(subcommands.values()) ) {
                CommandSpec spec = cl.getCommandSpec();
                if (spec.usageMessage().hidden() && !includeHidden) { continue; }
                var subsubcommands = spec.subcommands();
                if ( includeParents || subsubcommands==null || subsubcommands.isEmpty() ) {
                    printHelp(spec);
                }
                run(spec.subcommands());
            }
        }
    }

    private void printHelp(CommandSpec spec) {
        System.out.print("\n==========\n");
        CommandLine cl = spec.commandLine();
        cl.usage(cl.getOut());
    }
    
}
