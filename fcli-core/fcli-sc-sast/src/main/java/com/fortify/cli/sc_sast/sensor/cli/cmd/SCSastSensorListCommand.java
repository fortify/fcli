package com.fortify.cli.sc_sast.sensor.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SCSastSensorListCommand extends AbstractSSCBaseRequestOutputCommand {

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
