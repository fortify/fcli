package com.fortify.cli.ssc.custom_tag.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.custom_tag.cli.mixin.SSCCustomTagResolverMixin;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagDescriptor;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class SSCCustomTagDeleteCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Mixin private SSCCustomTagResolverMixin.PositionalParameterSingle customTagResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCCustomTagDescriptor desc = customTagResolver.getCustomTagDescriptor(unirest);
        unirest.delete("/api/v1/customTags/{id}").routeParam("id", desc.getId()).asString().getBody();
        return desc.asJsonNode();
    }

    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}