package com.fortify.cli.util._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.util.all_commands.cli.cmd.AllCommandsCommands;
import com.fortify.cli.util.autocomplete.cli.cmd.AutoCompleteCommands;
import com.fortify.cli.util.crypto.cli.cmd.CryptoCommands;
import com.fortify.cli.util.ncd_report.cli.cmd.NcdReportCommands;

import picocli.CommandLine.Command;

@Command(
        name = "util",
        resourceBundle = "com.fortify.cli.util.i18n.UtilMessages",
        subcommands = {
            AllCommandsCommands.class,
            AutoCompleteCommands.class,
            CryptoCommands.class,
            NcdReportCommands.class
        }
)
public class UtilCommands extends AbstractFortifyCLICommand {}
