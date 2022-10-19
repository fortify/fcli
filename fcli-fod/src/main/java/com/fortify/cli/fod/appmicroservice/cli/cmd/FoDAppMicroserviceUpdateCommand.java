package com.fortify.cli.fod.appmicroservice.cli.cmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.appmicroservice.cli.mixin.FoDAppMicroserviceResolverMixin;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceUpdateRequest;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelHelper;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelUpdateRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.util.ResourceBundle;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Update.CMD_NAME)
public class FoDAppMicroserviceUpdateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Update outputHelper;
    @Spec CommandSpec spec;
    ResourceBundle bundle = ResourceBundle.getBundle("com.fortify.cli.fod.i18n.FoDMessages");

    @Mixin private FoDAppMicroserviceResolverMixin.PositionalParameter appMicroserviceResolver;

    @Option(names = {"--name", "-n"}, required = true)
    private String microserviceName;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        FoDAppMicroserviceDescriptor appMicroserviceDescriptor;
        try {
            appMicroserviceDescriptor = FoDAppMicroserviceHelper.getAppMicroservice(unirest, appMicroserviceResolver.getAppAndMicroserviceNameOrId(), ":", true);
        } catch (JsonProcessingException e) {
            throw new ParameterException(spec.commandLine(),
                    bundle.getString("fcli.fod.microservice.update.invalid-parameter"));
        }

        FoDAppMicroserviceUpdateRequest msUpdateRequest = new FoDAppMicroserviceUpdateRequest()
                .setMicroserviceName(microserviceName);
        return FoDAppMicroserviceHelper.updateteAppMicroservice(unirest, appMicroserviceDescriptor, msUpdateRequest);

    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppMicroserviceHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
