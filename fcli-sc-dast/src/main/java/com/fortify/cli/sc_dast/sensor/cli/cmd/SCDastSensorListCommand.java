package com.fortify.cli.sc_dast.sensor.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastOutputCommand;
import com.fortify.cli.sc_dast.output.cli.mixin.SCDastOutputHelperMixins;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=SCDastOutputHelperMixins.List.CMD_NAME)
public class SCDastSensorListCommand extends AbstractSCDastOutputCommand implements IUnirestBaseRequestSupplier {
    @Getter @Mixin private SCDastOutputHelperMixins.List outputHelper;
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v2/scanners");
    };
}
