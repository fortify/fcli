package com.fortify.cli.fod.appmicroservice.cli.cmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.appmicroservice.cli.mixin.FoDAppMicroserviceResolverMixin;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceUpdateRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.util.ResourceBundle;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Create.CMD_NAME)
public class FoDAppMicroserviceCreateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Update outputHelper;
    @Mixin private FoDAppResolverMixin.PositionalParameter appResolver;

    @Option(names = {"--name", "-n"}, required = true)
    private String microserviceName;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        FoDAppDescriptor appDescriptor = FoDAppHelper.getApp(unirest, appResolver.getAppNameOrId(), true);
        FoDAppMicroserviceUpdateRequest msCreateRequest = new FoDAppMicroserviceUpdateRequest()
                .setMicroserviceName(microserviceName);
        return FoDAppMicroserviceHelper.createAppMicroservice(unirest, appDescriptor.getApplicationId(), msCreateRequest);

    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppMicroserviceHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
