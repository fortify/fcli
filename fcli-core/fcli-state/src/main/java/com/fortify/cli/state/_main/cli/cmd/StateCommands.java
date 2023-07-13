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
package com.fortify.cli.state._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.state.variable.cli.cmd.VariableCommands;

import picocli.CommandLine.Command;

@Command(
        name = "state",
        resourceBundle = "com.fortify.cli.state.i18n.StateMessages",
        subcommands = {
                StateClearCommand.class,
                VariableCommands.class
        }
)
public class StateCommands extends AbstractFortifyCLICommand {
}
