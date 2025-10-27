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
package com.fortify.cli.ssc.issue_template.cli.cmd;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.custom_tag.cli.mixin.SSCCustomTagAddRemoveMixin;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagUpdateHelper;
import com.fortify.cli.ssc.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateHelper;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public abstract class AbstractSSCIssueTemplateUpdateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Mixin protected SSCIssueTemplateResolverMixin.PositionalParameterSingle issueTemplateResolver;
    @Option(names={"--name","-n"}, required = false)
    protected String name;
    @Option(names={"--description","-d"}, required = false)
    protected String description;
    @Option(names={"--set-as-default"})
    protected boolean setAsDefault;
    @Mixin protected SSCCustomTagAddRemoveMixin.OptionalTagAddOption addTagsMixin;
    @Mixin protected SSCCustomTagAddRemoveMixin.OptionalTagRemoveOption rmTagsMixin;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCIssueTemplateDescriptor descriptor = issueTemplateResolver.getIssueTemplateDescriptor(unirest);
        ObjectNode updateData = (ObjectNode)descriptor.asJsonNode();
        if ( StringUtils.isNotBlank(name) ) { updateData.put("name", name); }
        if ( StringUtils.isNotBlank(description) ) { updateData.put("description", description); }
        if ( setAsDefault ) { updateData.put("defaultTemplate", true); }
        updateData.set("customTagIds", getCustomTagIds(unirest, descriptor));
        unirest.put(SSCUrls.ISSUE_TEMPLATE(descriptor.getId()))
            .body(updateData).asObject(JsonNode.class).getBody();
        return new SSCIssueTemplateHelper(unirest).getDescriptorByNameOrId(descriptor.getId(), true).asJsonNode();
    }

    protected ArrayNode getCustomTagIds(UnirestInstance unirest, SSCIssueTemplateDescriptor descriptor) {
        SSCCustomTagUpdateHelper tagUpdateHelper = new SSCCustomTagUpdateHelper(unirest);
        var currentTags = SSCIssueTemplateHelper.getCustomTagsRequest(unirest, descriptor.getId()).asObject(JsonNode.class).getBody();
        return tagUpdateHelper.computeUpdatedTagDescriptors(
                currentTags,
                addTagsMixin.getTagSpecs(),
                rmTagsMixin.getTagSpecs()
        ).map(tag -> new IntNode(Integer.valueOf(tag.getId()))).collect(JsonHelper.arrayNodeCollector());
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