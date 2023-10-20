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
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = "get-filter") @CommandGroup("filter")
public class SSCIssueFilterGetCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @EnvSuffix("FILTER") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.issue.filter")
    @Getter private String filter;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return new SSCIssueFilterHelper(unirest, parentResolver.getAppVersionId(unirest)).getFilterNode(filter);
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
