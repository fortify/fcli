/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.release.cli.cmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDAppAndRelNameDescriptor;
import com.fortify.cli.fod.release.cli.mixin.FoDAppAndRelNameResolverMixin;
import com.fortify.cli.fod.release.helper.FoDAppRelCreateRequest;
import com.fortify.cli.fod.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Create.CMD_NAME)
public class FoDAppRelCreateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Create outputHelper;
    @Spec CommandSpec spec;
    //ResourceBundle bundle = ResourceBundle.getBundle("com.fortify.cli.fod.i18n.FoDMessages");
    @Mixin private FoDAppAndRelNameResolverMixin.PositionalParameter appAndRelNameResolver;

    @Option(names = {"--description", "-d"})
    private String description;
    @Option(names = {"--copy-from"})
    private String copyReleaseNameOrId;
    @Option(names = {"--microservice"})
    private String microserviceNameOrId;
    @Option(names={"--skip-if-exists"})
    private boolean skipIfExists = false;

    @Mixin
    private FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;

    // TODO Consider splitting method
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if (skipIfExists) {
            FoDAppRelDescriptor descriptor = FoDAppRelHelper.getOptionalAppRelFromAppAndRelName(unirest, appAndRelNameResolver.getAppAndRelNameDescriptor());
            if (descriptor != null) { return descriptor.asObjectNode().put("__action__", "SKIPPED_EXISTING"); }
        }
        FoDAppAndRelNameDescriptor appAndRelNameDescriptor = FoDAppAndRelNameDescriptor.fromCombinedAppAndRelName(
                appAndRelNameResolver.getAppAndRelName(), appAndRelNameResolver.getDelimiter());

        int copyReleaseId = 0;
        int microServiceId = 0;
        boolean copyState = (copyReleaseNameOrId != null && !copyReleaseNameOrId.isEmpty());
        if (copyState) {
            copyReleaseId = FoDAppRelHelper.getAppRelDescriptor(unirest,appAndRelNameDescriptor.getAppName()+":"+copyReleaseNameOrId,
                    ":", true).getReleaseId();
        }
        if (microserviceNameOrId != null && !microserviceNameOrId.isEmpty()) {
            try {
                FoDAppMicroserviceDescriptor descriptor = FoDAppMicroserviceHelper.getAppMicroserviceDescriptor(unirest, appAndRelNameDescriptor.getAppName()+":"+ microserviceNameOrId, ":", true);
                microServiceId = descriptor.getMicroserviceId();
            } catch (JsonProcessingException e) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        "Unable to resolve application name and microservice name.");
            }
        }
        int appId = FoDAppHelper.getAppDescriptor(unirest, appAndRelNameDescriptor.getAppName(), true).getApplicationId();

        FoDAppRelCreateRequest relCreateRequest = new FoDAppRelCreateRequest()
                .setApplicationId(appId)
                .setReleaseName(appAndRelNameDescriptor.getRelName())
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
