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
package com.fortify.cli.ssc.custom_tag.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCCustomTagListCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.OptionalOption appVersionResolver;
    @Mixin private SSCIssueTemplateResolverMixin.OptionalOption issueTemplateResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var avNameOrId = appVersionResolver.getAppVersionNameOrId();
        var itNameOrId = issueTemplateResolver.getIssueTemplateNameOrId();
        boolean hasAv = avNameOrId!=null && !avNameOrId.isBlank();
        boolean hasIt = itNameOrId!=null && !((String)itNameOrId).isBlank();
        if ( hasAv && hasIt ) {
            throw new FcliSimpleException("Only one of --appversion/--av or --issue-template may be specified");
        }
        if ( hasAv ) {
            var avDesc = appVersionResolver.getAppVersionDescriptor(unirest, "id");
            return SSCAppVersionHelper.getCustomTagsRequest(unirest, avDesc.getVersionId()).asObject(JsonNode.class).getBody();
        }
        if ( hasIt ) {
            SSCIssueTemplateDescriptor itDesc = issueTemplateResolver.getIssueTemplateDescriptor(unirest);
            return SSCIssueTemplateHelper.getCustomTagsRequest(unirest, itDesc.getId()).asObject(JsonNode.class).getBody();
        }
        return unirest.get(SSCUrls.CUSTOM_TAGS).queryString("limit", "-1").asObject(JsonNode.class).getBody();
    }

    @Override
    public boolean isSingular() { return false; }
}