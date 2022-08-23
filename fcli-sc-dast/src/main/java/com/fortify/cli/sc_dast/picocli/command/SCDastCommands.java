package com.fortify.cli.sc_dast.picocli.command;

import com.fortify.cli.sc_dast.picocli.command.scan.SCDastScanCommands;
import com.fortify.cli.sc_dast.picocli.command.scan_output.SCDastScanOutputCommands;
import com.fortify.cli.sc_dast.picocli.command.scan_settings.SCDastScanSettingsCommands;
import com.fortify.cli.sc_dast.picocli.command.sensor.SCDastSensorCommands;
import com.fortify.cli.ssc.session.cli.SSCSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-dast",
        resourceBundle = "com.fortify.cli.sc_dast.i18n.SCDastMessages",
        subcommands = {
                SCDastScanCommands.class,
                SCDastScanOutputCommands.class,
                SCDastScanSettingsCommands.class,
                SCDastSensorCommands.class,
                SSCSessionCommands.class
        }
)
public class SCDastCommands {
        public SCDastCommands(){
                System.setProperty("productName", "ScanCentral DAST");
        }
}
