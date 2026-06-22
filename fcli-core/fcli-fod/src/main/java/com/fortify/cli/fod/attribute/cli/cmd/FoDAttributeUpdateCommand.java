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
package com.fortify.cli.fod.attribute.cli.cmd;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeResolverMixin;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDefinitionDescriptor;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDefinitionHelper;
import com.fortify.cli.fod.attribute.helper.FoDAttributeUpdateRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;


@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class FoDAttributeUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FoDAttributeResolverMixin.PositionalParameter attributeResolver;

    @Option(names = {"--required"})
    private Boolean isRequired;

    @Option(names = {"--restricted"})
    private Boolean isRestricted;

    @Option(names= {"--overwrite"})
    private Boolean overwriteExistingValues = false;

    @Option(names = {"--values", "--picklist-values"}, required = false, split = ",")
    private List<String> picklistValues;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        // current values of attribute being updated
        FoDAttributeDefinitionDescriptor attrDescriptor = new FoDAttributeDefinitionHelper(unirest).getDefinition(attributeResolver.getAttributeId(), true);

        // build request object
        FoDAttributeUpdateRequest request = FoDAttributeUpdateRequest.builder()
                .isRequired(isRequired)
                .isRestricted(isRestricted)
                .overwriteExistingValues(overwriteExistingValues)
                .picklistValues(picklistValues)
                .build();

        return new FoDAttributeDefinitionHelper(unirest).updateDefinition(String.valueOf(attrDescriptor.getId()), request).asJsonNode();
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
