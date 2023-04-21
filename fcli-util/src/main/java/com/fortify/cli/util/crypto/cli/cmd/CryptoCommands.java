package com.fortify.cli.util.crypto.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "crypto",
        subcommands = {
            CryptoEncryptCommand.class,
            CryptoDecryptCommand.class
        }
)
public class CryptoCommands extends AbstractFortifyCLICommand {}
