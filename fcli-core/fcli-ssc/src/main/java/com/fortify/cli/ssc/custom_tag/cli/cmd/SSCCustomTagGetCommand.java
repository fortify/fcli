package com.fortify.cli.ssc.custom_tag.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.custom_tag.cli.mixin.SSCCustomTagResolverMixin;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagDescriptor;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCCustomTagGetCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private SSCCustomTagResolverMixin.PositionalParameterSingle customTagResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCCustomTagDescriptor desc = customTagResolver.getCustomTagDescriptor(unirest);
        return desc.asJsonNode();
    }

    @Override
    public boolean isSingular() { return true; }
}