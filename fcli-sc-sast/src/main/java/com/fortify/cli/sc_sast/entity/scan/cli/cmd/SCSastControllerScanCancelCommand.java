package com.fortify.cli.sc_sast.entity.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.sc_sast.entity.scan.cli.mixin.SCSastScanJobResolverMixin;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobDescriptor;
import com.fortify.cli.sc_sast.output.cli.cmd.AbstractSCSastControllerJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Cancel.CMD_NAME)
public class SCSastControllerScanCancelCommand extends AbstractSCSastControllerJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Cancel outputHelper;
    @Mixin private SCSastScanJobResolverMixin.PositionalParameter scanJobResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SCSastControllerScanJobDescriptor descriptor = scanJobResolver.getScanJobDescriptor(unirest);
        String scanJobToken = descriptor.getJobToken();
        unirest.delete("/rest/v2/job/{token}")
                .routeParam("token", scanJobToken).asObject(JsonNode.class).getBody();
        return descriptor.asJsonNode(); // TODO Should we get the updated descriptor? (if still available after DELETE request)
    }
    
    @Override
    public String getActionCommandResult() {
        return "CANCEL_REQUESTED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
