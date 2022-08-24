package com.fortify.cli.sc_dast._main.cli;

import com.fortify.cli.sc_dast.scan.cli.SCDastScanCommands;
import com.fortify.cli.sc_dast.scan_output.cli.SCDastScanOutputCommands;
import com.fortify.cli.sc_dast.scan_settings.cli.SCDastScanSettingsCommands;
import com.fortify.cli.sc_dast.sensor.cli.SCDastSensorCommands;
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
