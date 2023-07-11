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
package com.fortify.cli.state.entity.variable.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "variable",
        aliases = "var",
        description = "Commands for managing fcli variables",
        subcommands = {
                VariableContentsCommand.class,
                VariableClearCommand.class,
                VariableDeleteCommand.class,
                VariableGetCommand.class,
                VariableListCommand.class
                
        }
)
public class VariableCommands extends AbstractFortifyCLICommand {
}
