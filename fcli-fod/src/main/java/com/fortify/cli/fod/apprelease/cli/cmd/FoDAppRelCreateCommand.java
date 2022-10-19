package com.fortify.cli.fod.apprelease.cli.cmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.appmicroservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelHelper;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelCreateRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.util.ResourceBundle;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Create.CMD_NAME)
public class FoDAppRelCreateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Create outputHelper;
    @Spec CommandSpec spec;
    ResourceBundle bundle = ResourceBundle.getBundle("com.fortify.cli.fod.i18n.FoDMessages");
    @Parameters(index = "0", arity = "1", descriptionKey = "appAndReleaseName")
    private String applicationName;
    @Parameters(index = "1", arity = "1", descriptionKey = "release-name")
    private String releaseName;
    @Option(names = {"--description", "-d"})
    private String description;
    @Option(names = {"--copy-state"})
    private Boolean copyState;
    @Option(names = {"--copy-release"})
    private String copyReleaseNameOrId;
    @Option(names = {"--microservice"})
    private String microserviceNameOrId;

    @Mixin
    private FoDSdlcStatusTypeOptions.RequiredSdlcOption sdlcStatus;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        Integer copyReleaseId = 0;
        Integer microServiceId = 0;
        if (copyState != null && copyState) {
            copyReleaseId = FoDAppRelHelper.getAppRel(unirest,applicationName+":"+copyReleaseNameOrId,
                    ":", true).getReleaseId();
        }
        if (microserviceNameOrId != null && !microserviceNameOrId.isEmpty()) {
            try {
                FoDAppMicroserviceDescriptor descriptor = FoDAppMicroserviceHelper.getAppMicroservice(unirest, applicationName+":"+ microserviceNameOrId, ":", true);
                microServiceId = descriptor.getMicroserviceId();
            } catch (JsonProcessingException e) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        bundle.getString("fcli.fod.microservice.update.invalid-parameter"));
            }
        }
        Integer appId = FoDAppHelper.getApp(unirest, applicationName, true).getApplicationId();

        FoDAppRelCreateRequest relCreateRequest = new FoDAppRelCreateRequest()
                .setApplicationId(appId)
                .setReleaseName(releaseName)
                .setReleaseDescription(description)
                .setCopyState(copyState)
                .setCopyStateReleaseId(copyReleaseId)
                .setSdlcStatusType(String.valueOf(sdlcStatus.getSdlcStatusType()))
                .setMicroserviceId(microServiceId);

        return FoDAppRelHelper.createAppRel(unirest, relCreateRequest).asJsonNode();
    }

    public JsonNode transformRecord(JsonNode record) {
        return FoDAppRelHelper.renameFields(record);
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
