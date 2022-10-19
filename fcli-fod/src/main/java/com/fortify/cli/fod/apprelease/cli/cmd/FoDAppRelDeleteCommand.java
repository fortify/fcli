package com.fortify.cli.fod.apprelease.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelHelper;
import com.fortify.cli.fod.apprelease.cli.mixin.FoDAppRelResolverMixin;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.rest.FoDUrls;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Delete.CMD_NAME)
public class FoDAppRelDeleteCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Delete outputHelper;
    @Mixin private FoDAppRelResolverMixin.PositionalParameter appRelResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDAppRelDescriptor appRelDescriptor = FoDAppRelHelper.getAppRel(unirest, appRelResolver.getAppRelNameOrId(), ":", true);
        unirest.delete(FoDUrls.RELEASE)
                .routeParam("relId", String.valueOf(appRelDescriptor.getReleaseId()))
                .asObject(JsonNode.class).getBody();
        return appRelDescriptor.asObjectNode();
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppRelHelper.renameFields(record);
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
