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
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseCreateRequest;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDReleaseCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameResolverMixin.PositionalParameter releaseNameResolver;
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.OptionalCopyFromOption copyFromReleaseResolver;

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
            var descriptor = releaseNameResolver.getReleaseDescriptor(unirest, false);
            if (descriptor != null) { 
                return descriptor.asObjectNode().put(IActionCommandResultSupplier.actionFieldName, "SKIPPED_EXISTING"); 
            }
        }
        // Ensure app exists
        var appDescriptor = releaseNameResolver.getAppDescriptor(unirest, true);
        // Ensure microservice exists (if specified)
        var microserviceDescriptor = releaseNameResolver.getMicroserviceDescriptor(unirest, true);
        // Ensure microservice is specified if application has microservices
        if ( appDescriptor.isHasMicroservices() && microserviceDescriptor==null ) {
            throw new IllegalArgumentException("Microservice name must be specified for microservices application");
        }
        
        String simpleReleaseName = releaseNameResolver.getSimpleReleaseName();
        String copyReleaseId = copyFromReleaseResolver.getReleaseId(unirest);

        var requestBuilder = FoDReleaseCreateRequest.builder()
                .applicationId(Integer.valueOf(appDescriptor.getApplicationId()))
                .releaseName(simpleReleaseName)
                .releaseDescription(description)
                .sdlcStatusType(sdlcStatus.getSdlcStatusType().name());
        if ( microserviceDescriptor!=null ) {
            requestBuilder = requestBuilder.microserviceId(Integer.valueOf(microserviceDescriptor.getMicroserviceId()));
        }
        if ( StringUtils.isNotBlank(copyReleaseId) ) {
            requestBuilder = requestBuilder
                .copyState(true)
                .copyStateReleaseId(Integer.parseInt(copyReleaseId));
        }

        return FoDReleaseHelper.createRelease(unirest, requestBuilder.build()).asJsonNode();
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
