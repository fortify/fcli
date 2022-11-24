package com.fortify.cli.ssc.appversion_vuln.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-vuln", aliases = "appversion-vulnerabilities",
        subcommands = {
            SSCAppVersionVulnCountCommand.class,
        }
)
public class SSCAppVersionVulnCommands extends AbstractFortifyCLICommand {
}
