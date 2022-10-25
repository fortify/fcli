package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.sc_sast.output.cli.cmd.AbstractSCSastControllerOutputCommand;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerOutputHelperMixins;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanJobResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "status")
public class SCSastControllerScanStatusCommand extends AbstractSCSastControllerOutputCommand implements IUnirestJsonNodeSupplier {
    @Getter @Mixin private SCSastControllerOutputHelperMixins.Cancel outputHelper;
    @Mixin SCSastScanJobResolverMixin.PositionalParameter scanJobResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return scanJobResolver.getScanJobDescriptor(unirest).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
