package com.fortify.cli.sc_dast.entity.sensor.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastBaseRequestOutputCommand;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=OutputHelperMixins.List.CMD_NAME)
public class SCDastSensorListCommand extends AbstractSCDastBaseRequestOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v2/scanners");
    };
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
