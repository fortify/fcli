/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.fod.action.cli.cmd;

import java.util.List;
import java.util.concurrent.Callable;

import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.Action.ActionMetadata;
import com.fortify.cli.common.action.model.ActionParameter;
import com.fortify.cli.common.action.runner.ActionParameterHelper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Spec;

@Command
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionCommand implements Callable<Integer> {
    private final Action action;
    @Spec
    private CommandSpec spec;

    private static final CommandSpec asCommandSpec(
            CommandSpec runCmd, // Represents applicable 'action run' command like 'fcli fod action run'
            Action action       // Action instance to be run
          ) {
            CommandSpec root = CommandSpec.create().resourceBundleBaseName("com.fortify.cli.common.i18n.FortifyCLIMessages");
            CommandSpec newRunCmd = replicateRunCmd(root, runCmd);
            CommandSpec actionCmd = CommandSpec.forAnnotatedObject(new ActionCommand(action));
            addUsage(actionCmd, action);
            addOptions(actionCmd, action);
            newRunCmd.addSubcommand(action.getMetadata().getName(), actionCmd);
            return actionCmd;
          }

    private static void addUsage(CommandSpec actionCmd, Action action) {
        actionCmd.usageMessage().header(action.getUsage().getHeader());
        actionCmd.usageMessage().description(action.getUsage().getDescription());
        
    }

    private static void addOptions(CommandSpec actionCmd, Action action) {
        for ( var o : ActionParameterHelper.getOptionDescriptors(action) ) {
            var optionSpec = OptionSpec.builder(o.getName(), o.getAliases().toArray(String[]::new))
                    .arity("1") // TODO arity 0..1 for boolean options, ...
                    .description(o.getDescription())
                    .build();
            actionCmd.addOption(optionSpec);
        }
    }

    private static CommandSpec replicateRunCmd(CommandSpec root, CommandSpec runCmd) {
        // TODO Iterate over runCmd and parents to replicate command structure
        var result = CommandSpec.create().name("run");
        result.parent(CommandSpec.create().name("action"));
        return result;
    }
    

    public static final CommandLine asCommandLine(
            CommandSpec runCmd, // Represents applicable 'action run' command like 'fcli fod action run'
            Action action       // Action instance to be run
          ) {
            CommandLine cl = new CommandLine(asCommandSpec(runCmd, action));
            //cl.setDefaultValueProvider(FortifyCLIDefaultValueProvider.getInstance());
            return cl;
          }

    public Integer call() {
        System.out.println(spec);
        spec.options().forEach(o->System.out.println(String.format("%s: %s", o.longestName(), o.getValue())));
        return 0;
    }
}
