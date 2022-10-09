package com.fortify.cli.sc_dast.scan_policy.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "scan-policy",
        subcommands = {
                SCDastScanPolicyGetCommand.class,
                SCDastScanPolicyListCommand.class
        }
)
public class SCDastScanPolicyCommands {
}
