package com.fortify.cli.sc_sast.picocli.command;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;

@Command(name="ping")
public class SCSastPingCommand extends AbstractSCSastUnirestRunnerCommand {

	@Override
	protected Void runWithUnirest(UnirestInstance unirest) {
		System.out.println(unirest.get("/rest/v2/ping").asObject(JsonNode.class).getBody());
		return null;
	}

}
