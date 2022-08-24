package com.fortify.cli.sc_sast.scan.cli;

import com.fortify.cli.sc_sast.rest.cli.AbstractSCSastUnirestRunnerCommand;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;

@Command(name = "start")
public class SCSastScanStartCommand extends AbstractSCSastUnirestRunnerCommand {

	@Override
	protected Void runWithUnirest(UnirestInstance unirest) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
