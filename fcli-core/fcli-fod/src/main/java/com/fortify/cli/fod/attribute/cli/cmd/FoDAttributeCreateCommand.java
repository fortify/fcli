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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeOptionCandidates;
import com.fortify.cli.fod.attribute.helper.FoDAttributeCreateRequest;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;
import com.fortify.cli.fod.attribute.helper.FoDPicklistSortedValue;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class FoDAttributeCreateCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @picocli.CommandLine.Mixin private OutputHelperMixins.Create outputHelper;

    @Option(names = {"--name"}, required = true)
    private String name;

    @Option(names = {"--type"},
            required = true,
            completionCandidates = FoDAttributeOptionCandidates.AttributeTypeCandidates.class
    )
    private String attributeType;

    @Option(
        names = {"--data-type"},
        required = true,
        completionCandidates = FoDAttributeOptionCandidates.AttributeDataTypeCandidates.class
    )
    private String attributeDataType;

    @Option(names = {"--required"})
    private boolean isRequired = false;

    @Option(names = {"--restricted"})
    private boolean isRestricted = false;

    @Option(names = {"--values"}, split = ",")
    private List<String> picklistValuesRaw;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();
        FoDAttributeCreateRequest attributeCreateRequest = FoDAttributeCreateRequest.builder()
                .name(name)
                .attributeType(attributeType)
                .attributeDataType(attributeDataType)
                .isRequired(isRequired)
                .isRestricted(isRestricted)
                .picklistValues(getPicklistValues())
                .build();
        return FoDAttributeHelper.createAttribute(unirest, attributeCreateRequest).asJsonNode();
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    private void validate() {
        if ("Picklist".equalsIgnoreCase(attributeDataType) && (picklistValuesRaw == null || picklistValuesRaw.isEmpty())) {
            throw new IllegalArgumentException("Picklist values must be specified when data type is Picklist.");
        }
    }

    private List<FoDPicklistSortedValue> getPicklistValues() {
        if (picklistValuesRaw == null) return null;
        List<FoDPicklistSortedValue> result = new ArrayList<>();
        int order = 0;
        for (String value : picklistValuesRaw) {
            result.add(new FoDPicklistSortedValue(value, order++));
        }
        return result;
    }

}