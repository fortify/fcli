package com.fortify.cli.sc_sast.sensor.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.sc_sast._common.output.cli.cmd.AbstractSCSastSSCBaseRequestOutputCommand;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SCSastSensorListCommand extends AbstractSCSastSSCBaseRequestOutputCommand {

    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/cloudworkers?orderby=scaVersion");
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }


}
