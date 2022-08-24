package com.fortify.cli.fod._main.cli;

import com.fortify.cli.fod.app.cli.FoDApplicationCommands;
import com.fortify.cli.fod.apprelease.cli.FoDApplicationReleaseCommands;
import com.fortify.cli.fod.dast_scan.cli.FoDDastScanCommands;
import com.fortify.cli.fod.sast_scan.cli.FoDSastScanCommands;
import com.fortify.cli.fod.session.cli.FoDSessionCommands;

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
