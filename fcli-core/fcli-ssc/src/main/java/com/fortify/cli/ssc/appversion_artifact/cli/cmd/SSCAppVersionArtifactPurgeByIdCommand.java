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
package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.appversion_artifact.cli.mixin.SSCAppVersionArtifactResolverMixin;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.ArtifactPurgeById.CMD_NAME)
public class SSCAppVersionArtifactPurgeByIdCommand extends AbstractSSCAppVersionArtifactOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.ArtifactPurgeById outputHelper;
    @Mixin private SSCAppVersionArtifactResolverMixin.PositionalParameter artifactResolver;
    
    @Override
    public JsonNode getJsonNode() {
        var unirest = getUnirestInstance();
        return SSCAppVersionArtifactHelper.purge(unirest, artifactResolver.getArtifactDescriptor(unirest)).asJsonNode();
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
