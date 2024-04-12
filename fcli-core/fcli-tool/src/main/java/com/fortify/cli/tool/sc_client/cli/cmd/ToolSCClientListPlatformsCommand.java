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
package com.fortify.cli.tool.sc_client.cli.cmd;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolListPlatformsCommand;

import lombok.Getter;
import picocli.CommandLine.Command;

@Command(name = "list-platforms", aliases = {"lsp"}) @CommandGroup("list-platforms")
public class ToolSCClientListPlatformsCommand extends AbstractToolListPlatformsCommand {
    @Getter private String toolName = ToolSCClientCommands.TOOL_NAME;
}
