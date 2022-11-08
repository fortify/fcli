package com.fortify.cli.sc_sast.ping.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.sc_sast.output.cli.cmd.AbstractSCSastControllerOutputCommand;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerOutputHelperMixins;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

// TODO Update or remove this command
@Command(name = SCSastControllerOutputHelperMixins.Ping.CMD_NAME)
public class SCSastPingCommand extends AbstractSCSastControllerOutputCommand implements IUnirestBaseRequestSupplier {
    @Getter @Mixin private SCSastControllerOutputHelperMixins.Ping outputHelper;
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/rest/v2/ping");
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
