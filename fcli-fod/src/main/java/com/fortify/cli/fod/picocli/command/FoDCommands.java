package com.fortify.cli.fod.picocli.command;

import com.fortify.cli.fod.picocli.command.application.FoDApplicationCommands;
import com.fortify.cli.fod.picocli.command.application.release.FoDApplicationReleaseCommands;
import com.fortify.cli.fod.picocli.command.dast_scan.FoDDastScanCommands;
import com.fortify.cli.fod.picocli.command.sast_scan.FoDSastScanCommands;
import com.fortify.cli.fod.picocli.command.session.FoDSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "fod",
        resourceBundle = "com.fortify.cli.fod.i18n.FoDMessages",
        subcommands = {
                FoDApplicationCommands.class,
                FoDApplicationReleaseCommands.class,
                FoDDastScanCommands.class,
                FoDSastScanCommands.class,
                FoDSessionCommands.class
                
        }
)
public class FoDCommands {
        public FoDCommands(){
                System.setProperty("productName", "FoD");
                System.setProperty("productLongName", "Fortify on Demand");
        }
}
