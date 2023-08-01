/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod.release.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseUpdateRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;


@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class FoDReleaseUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FoDReleaseResolverMixin.PositionalParameter releaseResolver;
    @Option(names = {"--name", "-n"})
    private String releaseName;

    @Option(names = {"--description", "-d"})
    private String description;

    @Option(names = {"--owner"})
    private String releaseOwner;

    @Mixin
    private FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        FoDSdlcStatusTypeOptions.FoDSdlcStatusType sdlcStatusTypeNew = sdlcStatus.getSdlcStatusType();

        FoDReleaseUpdateRequest appRelUpdateRequest = FoDReleaseUpdateRequest.builder()
                .releaseName(StringUtils.isNotBlank(releaseName) ? releaseName : releaseDescriptor.getReleaseName())
                .releaseDescription(StringUtils.isNotBlank(description) ? description : releaseDescriptor.getReleaseDescription())
                .ownerId(StringUtils.isNotBlank(releaseOwner) ? Integer.valueOf(releaseOwner) : releaseDescriptor.getOwnerId())
                .microserviceId(releaseDescriptor.getMicroserviceId())
                .sdlcStatusType(sdlcStatusTypeNew != null ? String.valueOf(sdlcStatusTypeNew) : releaseDescriptor.getSdlcStatusType()).build();

        return FoDReleaseHelper.updateRelease(unirest, releaseDescriptor.getReleaseId(), appRelUpdateRequest).asJsonNode();
    }
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDReleaseHelper.renameFields(record);
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
