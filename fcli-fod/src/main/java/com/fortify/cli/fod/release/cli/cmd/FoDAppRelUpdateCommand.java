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
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDAppRelResolverMixin;
import com.fortify.cli.fod.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.release.helper.FoDAppRelUpdateRequest;

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


@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Update.CMD_NAME)
public class FoDAppRelUpdateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Update outputHelper;
    @Mixin private FoDAppRelResolverMixin.PositionalParameter appRelResolver;
    @Spec CommandSpec spec;
    //ResourceBundle bundle = ResourceBundle.getBundle("com.fortify.cli.fod.i18n.FoDMessages");
    @Option(names = {"--name", "-n"})
    private String releaseName;

    @Option(names = {"--description", "-d"})
    private String description;

    @Option(names = {"--owner"})
    private String releaseOwner;

    @Option(names = {"--microservice"})
    private String microserviceNameOrId;

    @Mixin
    private FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        int microServiceId = 0;
        FoDAppRelDescriptor appRelDescriptor = appRelResolver.getAppRelDescriptor(unirest);
        if (microserviceNameOrId != null && !microserviceNameOrId.isEmpty()) {
            try {
                FoDAppMicroserviceDescriptor descriptor = FoDAppMicroserviceHelper.getAppMicroserviceDescriptor(unirest,
                        appRelDescriptor.getApplicationName()+":"+microserviceNameOrId, ":", true);
                microServiceId = descriptor.getMicroserviceId();
            } catch (JsonProcessingException e) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        "Unable to resolve application name and microservice name.");
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
