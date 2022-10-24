package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.sc_sast.output.cli.cmd.AbstractSCSastControllerOutputCommand;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerOutputHelperMixins;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanTokenMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SCSastControllerOutputHelperMixins.Cancel.CMD_NAME)
public class SCSastControllerScanCancelCommand extends AbstractSCSastControllerOutputCommand implements IUnirestBaseRequestSupplier {
    @Getter @Mixin private SCSastControllerOutputHelperMixins.Cancel outputHelper;
    @Mixin private SCSastScanTokenMixin scanStatusOptions;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        // TODO Generate proper output
        return unirest.delete("/rest/v2/job/{token}")
                .routeParam("token", scanStatusOptions.getToken());
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
