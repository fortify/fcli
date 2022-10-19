package com.fortify.cli.fod.apprelease.cli.cmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.apprelease.cli.mixin.FoDAppRelResolverMixin;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelHelper;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelUpdateRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.ResourceBundle;


@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Update.CMD_NAME)
public class FoDAppRelUpdateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Update outputHelper;
    @Mixin private FoDAppRelResolverMixin.PositionalParameter appRelResolver;
    @Spec CommandSpec spec;
    ResourceBundle bundle = ResourceBundle.getBundle("com.fortify.cli.fod.i18n.FoDMessages");
    @Option(names = {"--name", "-n"})
    private String releaseName;

    @Option(names = {"--description", "-d"})
    private String description;

    @Option(names = {"--owner"})
    private String releaseOwner;

    @Option(names = {"--microservice"})
    private String microserviceNameOrId;

    @Mixin
    private FoDSdlcStatusTypeOptions.RequiredSdlcOption sdlcStatus;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        Integer microServiceId = 0;
        FoDAppRelDescriptor appRelDescriptor = FoDAppRelHelper.getAppRel(unirest, appRelResolver.getAppRelNameOrId(), ":", true);
        if (microserviceNameOrId != null && !microserviceNameOrId.isEmpty()) {
            try {
                FoDAppMicroserviceDescriptor descriptor = FoDAppMicroserviceHelper.getAppMicroservice(unirest,
                        appRelDescriptor.getApplicationName()+":"+microserviceNameOrId, ":", true);
                microServiceId = descriptor.getMicroserviceId();
            } catch (JsonProcessingException e) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        bundle.getString("fcli.fod.microservice.update.invalid-parameter"));
            }
        }

        FoDSdlcStatusTypeOptions.FoDSdlcStatusType sdlcStatusTypeNew = sdlcStatus.getSdlcStatusType();

        FoDAppRelUpdateRequest appRelUpdateRequest = new FoDAppRelUpdateRequest()
                .setReleaseName(releaseName != null && StringUtils.isNotEmpty(releaseName) ? releaseName : appRelDescriptor.getReleaseName())
                .setReleaseDescription(description != null && StringUtils.isNotEmpty(description) ? description : appRelDescriptor.getReleaseDescription())
                .setOwnerId(releaseOwner != null && StringUtils.isNotEmpty(releaseOwner) ? Integer.valueOf(releaseOwner) : appRelDescriptor.getOwnerId())
                .setMicroserviceId(microServiceId)
                .setSdlcStatusType(sdlcStatusTypeNew != null ? String.valueOf(sdlcStatusTypeNew) : appRelDescriptor.getSdlcStatusType());

        return FoDAppRelHelper.updateAppRel(unirest, appRelDescriptor.getReleaseId(), appRelUpdateRequest).asJsonNode();
    }
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppRelHelper.renameFields(record);
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
