package com.fortify.cli.fod.apprelease.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.apprelease.FoDAppRelHelper;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelCreateRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Create.CMD_NAME)
public class FoDAppRelCreateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Create outputHelper;

    @Parameters(index = "0", arity = "1", descriptionKey = "application-name")
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
    private String microServiceNameOrId;

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
        if (microServiceNameOrId != null && !microServiceNameOrId.isEmpty()) {
            // TODO: lookup microservice id;
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
