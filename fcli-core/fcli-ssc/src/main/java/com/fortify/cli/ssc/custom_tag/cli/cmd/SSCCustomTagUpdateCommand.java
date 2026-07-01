/*
 * Copyright 2021-2026 Open Text.
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.custom_tag.cli.mixin.SSCCustomTagResolverMixin;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagDefinitionHelper;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class SSCCustomTagUpdateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private SSCCustomTagResolverMixin.PositionalParameterSingle customTagResolver;
    @Option(names = {"--name"}, required = false)
    private String name;
    @Option(names = {"-d", "--description"}, required = false)
    private String description;
    @Option(names = {"--values"}, required = false)
    private String values;
    @Option(names = {"--add-values"}, required = false)
    private String addValues;
    @Option(names = {"--rm-values"}, required = false)
    private String rmValues;
    @Option(names = {"--restricted"}, required = false, negatable = true)
    private Boolean restriction;
    @Option(names = {"--hidden"}, required = false, negatable = true)
    private Boolean hidden;
    @Option(names = {"--requires-comment"}, required = false, negatable = true)
    private Boolean requiresComment;
    @Option(names = {"--extensible"}, required = false, negatable = true)
    private Boolean extensible;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCCustomTagDefinitionHelper customTagDefinitionHelper = new SSCCustomTagDefinitionHelper(unirest);
        SSCCustomTagDescriptor desc = customTagResolver.getCustomTagDescriptor(unirest);
        ObjectNode updateData = customTagDefinitionHelper.buildUpdateBody(
                desc, name, description, restriction, hidden, requiresComment, extensible, values, addValues, rmValues);
        unirest.put("/api/v1/customTags/{id}")
            .routeParam("id", desc.getId())
            .body(updateData).asObject(JsonNode.class).getBody();
        return customTagDefinitionHelper.getDescriptorByCustomTagSpec(desc.getGuid(), true).asJsonNode();
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