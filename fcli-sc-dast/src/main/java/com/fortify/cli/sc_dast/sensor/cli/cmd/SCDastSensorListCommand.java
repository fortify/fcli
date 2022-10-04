package com.fortify.cli.sc_dast.sensor.cli.cmd;

import com.fortify.cli.sc_dast.rest.cli.cmd.AbstractSCDastHttpListCommand;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;

@Command(name=SCDastSensorListCommand.CMD_NAME)
public class SCDastSensorListCommand extends AbstractSCDastHttpListCommand {
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v2/scanners");
    };
}
