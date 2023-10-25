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
package com.fortify.cli.ssc.artifact.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.artifact.cli.mixin.SSCArtifactResolverMixin;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "purge")
public class SSCArtifactPurgeCommand extends AbstractSSCArtifactOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private SSCArtifactResolverMixin.PositionalParameter artifactResolver;
    
    @Override
    public JsonNode getJsonNode() {
        var unirest = getUnirestInstance();
        return SSCArtifactHelper.purge(unirest, artifactResolver.getArtifactDescriptor(unirest)).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "PURGE_REQUESTED";
    }
}
