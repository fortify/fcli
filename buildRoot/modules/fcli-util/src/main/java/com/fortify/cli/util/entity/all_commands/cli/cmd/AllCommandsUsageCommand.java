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
package com.fortify.cli.util.entity.all_commands.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.util.entity.all_commands.cli.mixin.AllCommandsCommandSelectorMixin;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "usage")
public final class AllCommandsUsageCommand extends AbstractFortifyCLICommand implements Runnable {
    @Mixin private AllCommandsCommandSelectorMixin selectorMixin;
    
    @Override
    public final void run() {
        initMixins();
        selectorMixin.getSelectedCommands().getSpecs()
            .forEach(this::printHelp);
    }

    private void printHelp(CommandSpec spec) {
        System.out.print("\n==========\n");
        CommandLine cl = spec.commandLine();
        cl.usage(cl.getOut());
    }
    
}
