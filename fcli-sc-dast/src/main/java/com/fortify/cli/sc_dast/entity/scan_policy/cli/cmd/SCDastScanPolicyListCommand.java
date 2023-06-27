/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_dast.entity.scan_policy.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.sc_dast.entity.scan_policy.helper.SCDastScanPolicyHelper;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SCDastScanPolicyListCommand extends AbstractSCDastJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return SCDastScanPolicyHelper.getScanPolicies(unirest);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
