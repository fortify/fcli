/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.tool._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.tool.fod_uploader.cli.cmd.ToolFoDUploaderCommands;
import com.fortify.cli.tool.sc_client.cli.cmd.ToolSCClientCommands;
import com.fortify.cli.tool.vuln_exporter.cli.cmd.ToolVulnExporterCommands;

import picocli.CommandLine.Command;

@Command(
        name = "tool",
        resourceBundle = "com.fortify.cli.tool.i18n.ToolMessages",
        subcommands = {
            ToolFoDUploaderCommands.class,
            ToolSCClientCommands.class,
            ToolVulnExporterCommands.class
        }
)
public class ToolCommands extends AbstractFortifyCLICommand {}
