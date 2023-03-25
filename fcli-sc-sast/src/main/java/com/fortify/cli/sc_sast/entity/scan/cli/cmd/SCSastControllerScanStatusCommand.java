package com.fortify.cli.sc_sast.entity.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.sc_sast.entity.scan.cli.mixin.SCSastScanJobResolverMixin;
import com.fortify.cli.sc_sast.output.cli.cmd.AbstractSCSastControllerJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Status.CMD_NAME)
public class SCSastControllerScanStatusCommand extends AbstractSCSastControllerJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Status outputHelper;
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
