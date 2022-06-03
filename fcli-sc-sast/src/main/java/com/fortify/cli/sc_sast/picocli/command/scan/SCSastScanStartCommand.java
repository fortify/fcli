package com.fortify.cli.sc_sast.picocli.command.scan;

import com.fortify.cli.sc_sast.picocli.command.AbstractSCSastUnirestRunnerCommand;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;

@Command(name = "start",
        description = "Start a ScanCentral SAST scan."
)
public class SCSastScanStartCommand extends AbstractSCSastUnirestRunnerCommand {

	@Override
	protected Void runWithUnirest(UnirestInstance unirest) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
