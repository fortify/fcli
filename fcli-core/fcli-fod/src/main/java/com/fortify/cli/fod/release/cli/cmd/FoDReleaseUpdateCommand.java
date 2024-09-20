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

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.app.attr.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.app.attr.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.app.attr.helper.FoDAttributeHelper;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.PositionalParameter releaseResolver;
    @Option(names = {"--name", "-n"})
    private String releaseName;

    @Option(names = {"--description", "-d"})
    private String description;

    @Option(names = {"--owner"})
    private String releaseOwner;

    @Mixin
    private FoDSdlcStatusTypeOptions.OptionalOption sdlcStatus;
    @Mixin 
    private FoDAttributeUpdateOptions.OptionalAttrOption appAttrsUpdate;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        ArrayList<FoDAttributeDescriptor> releaseAttrsCurrent = releaseDescriptor.getAttributes();
        FoDSdlcStatusTypeOptions.FoDSdlcStatusType sdlcStatusTypeNew = sdlcStatus.getSdlcStatusType();
        Map<String, String> attributeUpdates = appAttrsUpdate.getAttributes();
        JsonNode jsonAttrs = objectMapper.createArrayNode();
        if (attributeUpdates != null && !attributeUpdates.isEmpty()) {
            jsonAttrs = FoDAttributeHelper.mergeAttributesNode(unirest, FoDEnums.AttributeTypes.Release, 
                releaseAttrsCurrent, attributeUpdates);
        } else {
            jsonAttrs = FoDAttributeHelper.getAttributesNode(FoDEnums.AttributeTypes.Release, releaseAttrsCurrent);
        }
        FoDReleaseUpdateRequest appRelUpdateRequest = FoDReleaseUpdateRequest.builder()
                .releaseName(StringUtils.isNotBlank(releaseName) ? getUnqualifiedReleaseName(releaseName, releaseDescriptor) : releaseDescriptor.getReleaseName())
                .releaseDescription(StringUtils.isNotBlank(description) ? description : releaseDescriptor.getReleaseDescription())
                .ownerId(StringUtils.isNotBlank(releaseOwner) ? Integer.valueOf(releaseOwner) : releaseDescriptor.getOwnerId())
                .microserviceId(releaseDescriptor.getMicroserviceId())
                .sdlcStatusType(sdlcStatusTypeNew != null ? String.valueOf(sdlcStatusTypeNew) : releaseDescriptor.getSdlcStatusType())
                .attributes(jsonAttrs).build();

        return FoDReleaseHelper.updateRelease(unirest, releaseDescriptor.getReleaseId(), appRelUpdateRequest).asJsonNode();
    }
    
    private String getUnqualifiedReleaseName(String potentialQualifiedName, FoDReleaseDescriptor descriptor) {
        if ( StringUtils.isBlank(potentialQualifiedName) ) { return null; }
        var delim = delimiterMixin.getDelimiter();
        var qualifierPrefix = descriptor.getQualifierPrefix(delim);
        var result = !potentialQualifiedName.startsWith(qualifierPrefix)
                ? potentialQualifiedName
                : potentialQualifiedName.substring(qualifierPrefix.length());
        if ( result.contains(delim) ) {
            throw new IllegalArgumentException(String.format("--name option must contain either a plain name or %s<new name>, current: %s", qualifierPrefix, potentialQualifiedName));
        }
        return result;
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
