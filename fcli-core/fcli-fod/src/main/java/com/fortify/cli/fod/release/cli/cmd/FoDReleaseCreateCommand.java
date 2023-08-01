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
import com.fortify.cli.fod.release.cli.mixin.FoDCopyFromReleaseResolverMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDQualifiedReleaseNameResolverMixin;
import com.fortify.cli.fod.release.helper.FoDQualifiedReleaseNameDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseCreateRequest;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDReleaseCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private FoDQualifiedReleaseNameResolverMixin.PositionalParameter releaseNameResolver;
    @Mixin private FoDCopyFromReleaseResolverMixin copyFromReleaseResolver;

    @Option(names = {"--description", "-d"})
    private String description;
    @Option(names={"--skip-if-exists"})
    private boolean skipIfExists = false;

    @Mixin
    private FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;

    // TODO Consider splitting method
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if (skipIfExists) {
            var descriptor = releaseNameResolver.getReleaseDescriptor(unirest);
            if (descriptor != null) { 
                return descriptor.asObjectNode().put(IActionCommandResultSupplier.actionFieldName, "SKIPPED_EXISTING"); 
            }
        }
        FoDQualifiedReleaseNameDescriptor qualifiedNameDescriptor = releaseNameResolver.getQualifiedReleaseNameDescriptor();

        String copyReleaseId = copyFromReleaseResolver.getReleaseId(unirest, releaseNameResolver.getDelimiterMixin());
        int appId = releaseNameResolver.getAppDescriptor(unirest, true).getApplicationId();
        var microserviceDescriptor = releaseNameResolver.getMicroServiceDescriptor(unirest, false);
        Integer microserviceId = microserviceDescriptor==null ? null : microserviceDescriptor.getMicroserviceId();                
        FoDReleaseCreateRequest relCreateRequest = FoDReleaseCreateRequest.builder()
                .applicationId(appId)
                .releaseName(qualifiedNameDescriptor.getReleaseName())
                .releaseDescription(description)
                .copyState(StringUtils.isNotBlank(copyReleaseId))
                .copyStateReleaseId(Integer.parseInt(copyReleaseId))
                .sdlcStatusType(sdlcStatus.getSdlcStatusType().name())
                .microserviceId(microserviceId).build();

        return FoDReleaseHelper.createRelease(unirest, relCreateRequest).asJsonNode();
    }

    public JsonNode transformRecord(JsonNode record) {
        return FoDReleaseHelper.renameFields(record);
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
