package com.fortify.cli.fod.apprelease.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.apprelease.FoDAppRelHelper;
import com.fortify.cli.fod.apprelease.cli.mixin.FoDAppRelResolverMixin;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelDescriptor;
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


@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Update.CMD_NAME)
public class FoDAppRelUpdateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Update outputHelper;
    @Mixin private FoDAppRelResolverMixin.PositionalParameter appRelResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // current values of app rel being updated
        FoDAppRelDescriptor appRelDescriptor = FoDAppRelHelper.getAppRel(unirest, appRelResolver.getAppRelNameOrId(), ":", true);
        // TODO: lookup microservice name or id

        // new values to replace
        FoDSdlcStatusTypeOptions.FoDSdlcStatusType sdlcStatusTypeNew = sdlcStatus.getSdlcStatusType();

        FoDAppRelUpdateRequest appRelUpdateRequest = new FoDAppRelUpdateRequest()
                .setReleaseName(releaseName != null && StringUtils.isNotEmpty(releaseName) ? releaseName : appRelDescriptor.getReleaseName())
                .setReleaseDescription(description != null && StringUtils.isNotEmpty(description) ? description : appRelDescriptor.getReleaseDescription())
                .setOwnerId(releaseOwner != null && StringUtils.isNotEmpty(releaseOwner) ? Integer.valueOf(releaseOwner) : appRelDescriptor.getOwnerId())
                .setMicroserviceId(microserviceNameOrId != null && StringUtils.isNotEmpty(microserviceNameOrId) ? Integer.valueOf(microserviceNameOrId) : appRelDescriptor.getMicroserviceId())
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
