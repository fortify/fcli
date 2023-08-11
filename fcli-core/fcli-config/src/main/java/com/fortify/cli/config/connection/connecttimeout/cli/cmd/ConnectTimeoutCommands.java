/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.config.connection.connecttimeout.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = "connecttimeout",
        subcommands = {
            ConnectTimeoutAddCommand.class,
            ConnectTimeoutClearCommand.class,
            ConnectTimeoutDeleteCommand.class,
            ConnectTimeoutListCommand.class,
            ConnectTimeoutUpdateCommand.class
        }
)
public class ConnectTimeoutCommands extends AbstractContainerCommand {
}
