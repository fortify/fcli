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
package com.fortify.cli.aviator.ssc.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCPrepareOptionsMixin;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCPrepareHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "prepare")
public class AviatorSSCPrepareCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;

    @ArgGroup(multiplicity = "1..*", heading = "%nUpdate Options (at least one is required):%n")
    private AviatorSSCPrepareOptionsMixin updateOptions;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        AviatorSSCPrepareHelper.PrepareOptions options = AviatorSSCPrepareHelper.PrepareOptions.builder()
                .issueTemplateNameOrId(updateOptions.getIssueTemplateNameOrId())
                .allIssueTemplates(updateOptions.isAllIssueTemplates())
                .appVersionNameOrId(updateOptions.getAppVersionNameOrId())
                .allAppVersions(updateOptions.isAllAppVersions())
                .build();

        AviatorSSCPrepareHelper helper = new AviatorSSCPrepareHelper(unirest);
        return helper.prepare(options).toJsonNode();
    }

    @Override
    public String getActionCommandResult() {
        return "PREPARED";
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}