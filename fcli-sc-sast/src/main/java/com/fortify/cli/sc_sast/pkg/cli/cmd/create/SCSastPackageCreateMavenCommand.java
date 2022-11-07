package com.fortify.cli.sc_sast.pkg.cli.cmd.create;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.sc_sast.output.cli.cmd.AbstractSCSastControllerOutputCommand;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerOutputHelperMixins;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "maven")
public class SCSastPackageCreateMavenCommand extends AbstractSCSastControllerOutputCommand implements IUnirestJsonNodeSupplier {
    @Getter @Mixin private SCSastControllerOutputHelperMixins.Other outputHelper;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        throw new RuntimeException("Not yet implemented");
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
