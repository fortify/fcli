/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod.release.cli.cmd;

import static com.fortify.cli.common.util.DisableTest.TestType.MULTI_OPT_PLURAL_NAME;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions.FoDAppType;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions.FoDAppTypeIterable;
import com.fortify.cli.fod.app.cli.mixin.FoDCriticalityTypeOptions.FoDCriticalityType;
import com.fortify.cli.fod.app.cli.mixin.FoDCriticalityTypeOptions.FoDCriticalityTypeIterable;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDMicroserviceHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseCreateRequest;
import com.fortify.cli.fod.release.helper.FoDReleaseCreateRequest.FoDReleaseCreateRequestBuilder;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDReleaseCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(FoDReleaseCreateCommand.class);
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameResolverMixin.PositionalParameter releaseNameResolver;
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.OptionalCopyFromOption copyFromReleaseResolver;
    @Mixin private CommonOptionMixins.SkipIfExistsOption skipIfExistsOption;
    @Mixin private CommonOptionMixins.AutoRequiredAttrsOption autoRequiredAttrsOption;

    @Option(names = {"--description", "-d"})
    private String description;

    @Mixin private FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;
    @Mixin private FoDAttributeUpdateOptions.OptionalAttrOption relAttrs;

    @ArgGroup(exclusive = false, headingKey = "fcli.fod.release.create.app-options") 
    private FoDReleaseAppCreateOptionsArgGroup appCreateOptions = new FoDReleaseAppCreateOptionsArgGroup();

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if (skipIfExistsOption.isSkipIfExists()) {
            var descriptor = releaseNameResolver.getReleaseDescriptor(unirest, false);
            if (descriptor != null) {
                return addActionCommandResult(descriptor.asObjectNode(), false, false, false);
            }
        }
        var appDescriptor = releaseNameResolver.getAppDescriptor(unirest, false);
        if ( appDescriptor==null ) { // If app doesn't exist yet, create app, microservice & release
            return createAppWithRelease(unirest);
        } else { // If app exists, create microservice if necessary, then create release
            return createReleaseWithOptionalMicroservice(unirest, appDescriptor); 
        } 
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
    
    private final JsonNode createAppWithRelease(UnirestInstance unirest) {
        if ( StringUtils.isNotBlank(copyFromReleaseResolver.getQualifiedReleaseNameOrId()) ) {
            LOG.warn("WARN: Ignoring --copy-from option as this is the first release on a new application");
        }
        var releaseNameDescriptor = releaseNameResolver.getQualifiedReleaseNameDescriptor();
        FoDAppCreateRequest appCreateRequest = FoDAppCreateRequest.builder()
                .applicationDescription(appCreateOptions.getAppDescription())
                .businessCriticality(appCreateOptions.getCriticalityType())
                .notify(appCreateOptions.getAppNotifications())
                .releaseNameDescriptor(releaseNameDescriptor)
                .releaseDescription(description)
                .sdlcStatus(sdlcStatus.getSdlcStatusType())
                .owner(unirest, appCreateOptions.getAppOwner())
                .appType(appCreateOptions.getAppType())
                .autoAttributes(unirest, relAttrs.getAttributes(), autoRequiredAttrsOption.isAutoRequiredAttrs())
                .userGroups(unirest, appCreateOptions.getAppUserGroups())
                .build().validate();
        FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
        var rel = FoDReleaseHelper.getReleaseDescriptor(unirest, releaseNameDescriptor.getQualifiedName(), delimiterMixin.getDelimiter(), true);
        return addActionCommandResult(rel.asObjectNode(), true, appCreateOptions.getAppType().equals(FoDAppType.Microservice), true);
    }
    
    private final ObjectNode createReleaseWithOptionalMicroservice(UnirestInstance unirest, FoDAppDescriptor appDescriptor) {
        boolean msCreated = false;
        var microserviceDescriptor = releaseNameResolver.getMicroserviceDescriptor(unirest, false);
        if ( microserviceDescriptor==null && appDescriptor.isHasMicroservices() ) { 
            String microserviceName = releaseNameResolver.getQualifiedReleaseNameDescriptor().getMicroserviceName();
            if ( StringUtils.isBlank(microserviceName) ) {
                throw new FcliSimpleException("Microservice name must be specified for microservices application");
            }
            microserviceDescriptor = FoDMicroserviceHelper.createMicroservice(unirest, appDescriptor, releaseNameResolver.getQualifiedReleaseNameDescriptor().getMicroserviceName(), relAttrs, autoRequiredAttrsOption.isAutoRequiredAttrs());
            msCreated = true;
        }
        return createRelease(unirest, appDescriptor, microserviceDescriptor, msCreated);
    }

    private final ObjectNode createRelease(UnirestInstance unirest, FoDAppDescriptor appDescriptor, FoDMicroserviceDescriptor microserviceDescriptor, boolean msCreated) {
        String simpleReleaseName = releaseNameResolver.getSimpleReleaseName();

        var requestBuilder = FoDReleaseCreateRequest.builder()
                .applicationId(Integer.valueOf(appDescriptor.getApplicationId()))
                .releaseName(simpleReleaseName)
                .releaseDescription(description)
                .sdlcStatusType(sdlcStatus.getSdlcStatusType().name())
                .attributes(FoDAttributeHelper.getAttributesNode(unirest, FoDEnums.AttributeTypes.Release, 
                    relAttrs.getAttributes(), autoRequiredAttrsOption.isAutoRequiredAttrs()));
        requestBuilder = addMicroservice(microserviceDescriptor, requestBuilder);
        requestBuilder = addCopyFrom(unirest, appDescriptor, requestBuilder);

        var rel = FoDReleaseHelper.createRelease(unirest, requestBuilder.build()).asObjectNode();
        return addActionCommandResult(rel, false, msCreated, true);
    }

    private FoDReleaseCreateRequestBuilder addCopyFrom(UnirestInstance unirest, FoDAppDescriptor appDescriptor, FoDReleaseCreateRequestBuilder requestBuilder) {
        var copyFromReleaseDescriptor = getCopyFromReleaseDescriptor(unirest, appDescriptor);
        if ( copyFromReleaseDescriptor!=null ) {
            requestBuilder = requestBuilder
                .copyState(true)
                .copyStateReleaseId(Integer.parseInt(copyFromReleaseDescriptor.getReleaseId()));
        }
        return requestBuilder;
    }
    
    private final FoDReleaseDescriptor getCopyFromReleaseDescriptor(UnirestInstance unirest, FoDAppDescriptor appDescriptor) {
        var copyFromReleaseNameDescriptor = copyFromReleaseResolver.getQualifiedReleaseNameDescriptor();
        if ( copyFromReleaseNameDescriptor!=null ) {
            if ( copyFromReleaseNameDescriptor.getQualifiedName().equals(releaseNameResolver.getQualifiedReleaseName()) ) {
                // Ignore --copy-from if pointing to same release as the release to be created
                LOG.warn("WARN: Ignoring --copy-from option as it's the same as the release being created");
                return null;
            }
        }
        var copyFromReleaseDescriptor = copyFromReleaseResolver.getReleaseDescriptor(unirest);
        if ( copyFromReleaseDescriptor!=null && !copyFromReleaseDescriptor.getApplicationId().equals(appDescriptor.getApplicationId()) ) {
            throw new FcliSimpleException("Copy release from different application is not allowed");
        }
        return copyFromReleaseDescriptor;
    }

    private FoDReleaseCreateRequestBuilder addMicroservice(FoDMicroserviceDescriptor microserviceDescriptor,
            FoDReleaseCreateRequestBuilder requestBuilder) {
        if ( microserviceDescriptor!=null ) {
            requestBuilder = requestBuilder.microserviceId(Integer.valueOf(microserviceDescriptor.getMicroserviceId()));
        }
        return requestBuilder;
    }
    
    private final ObjectNode addActionCommandResult(ObjectNode rel, boolean appCreated, boolean msCreated, boolean relCreated) {
        var result = new ArrayList<String>();
        addActionCommandResult(result, appCreated, "APP_CREATED");
        addActionCommandResult(result, msCreated,  "MICROSERVICE_CREATED");
        addActionCommandResult(result, relCreated, "RELEASE_CREATED");
        if ( result.isEmpty() ) { result.add("SKIPPED_EXISTING"); }
        return rel.put(IActionCommandResultSupplier.actionFieldName, String.join("\n", result));
    }
    
    private final void addActionCommandResult(ArrayList<String> result, boolean add, String value) {
        if ( add ) { result.add(value); }
    }
    
    @Getter
    public static final class FoDReleaseAppCreateOptionsArgGroup {
        @Option(names = {"--app-description"}, required = false)
        protected String appDescription;
        @DisableTest(MULTI_OPT_PLURAL_NAME)
        @Option(names = {"--app-notify"}, required = false, split=",")
        protected ArrayList<String> appNotifications;
        @Option(names = {"--app-owner"}, required = false)
        protected String appOwner;
        @Option(names = {"--app-groups"}, required = false, split=",")
        protected ArrayList<String> appUserGroups;
        @Option(names = {"--app-type"}, required = false, completionCandidates = FoDAppTypeIterable.class)
        private FoDAppType appType;
        @Option(names = {"--app-criticality"}, required = false, completionCandidates = FoDCriticalityTypeIterable.class)
        private FoDCriticalityType criticalityType;
    }
}
