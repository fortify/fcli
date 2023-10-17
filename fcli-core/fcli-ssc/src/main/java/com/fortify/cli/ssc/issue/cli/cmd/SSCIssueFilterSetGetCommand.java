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
package com.fortify.cli.ssc.issue.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.GetFilterSet.CMD_NAME) @CommandGroup("filter-set")
public class SSCIssueFilterSetGetCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private SSCOutputHelperMixins.GetFilterSet outputHelper; 
    @Mixin SSCIssueFilterSetResolverMixin.PositionalParameterSingle filterSetResolver;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return filterSetResolver.getFilterSetDescriptor(unirest, parentResolver.getAppVersionId(unirest)).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
