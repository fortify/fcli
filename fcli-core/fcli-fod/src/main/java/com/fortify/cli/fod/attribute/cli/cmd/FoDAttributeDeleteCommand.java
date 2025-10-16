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
package com.fortify.cli.fod.attribute.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeResolverMixin;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class FoDAttributeDeleteCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Mixin private FoDAttributeResolverMixin.PositionalParameter attributeResolver;

    @Override
    public JsonNode getJsonNode (UnirestInstance unirest){
        FoDAttributeDescriptor attrDescriptor = FoDAttributeHelper.getAttributeDescriptor(unirest, attributeResolver.getAttributeId(), true);
        unirest.delete(FoDUrls.ATTRIBUTE)
                .routeParam("attributeId", String.valueOf(attrDescriptor.getId()))
                .asObject(JsonNode.class).getBody();
        return attrDescriptor != null ? attrDescriptor.asObjectNode() : null;
    }

    @Override
    public String getActionCommandResult () {
        return "DELETED";
    }

    @Override
    public boolean isSingular () {
        return true;
    }
}