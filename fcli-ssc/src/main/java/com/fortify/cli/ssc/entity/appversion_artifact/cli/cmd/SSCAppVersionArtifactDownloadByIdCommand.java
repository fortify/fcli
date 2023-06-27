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
package com.fortify.cli.ssc.entity.appversion_artifact.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.entity.appversion_artifact.cli.mixin.SSCAppVersionArtifactDownloadOptions;
import com.fortify.cli.ssc.entity.appversion_artifact.cli.mixin.SSCAppVersionArtifactResolverMixin;
import com.fortify.cli.ssc.entity.appversion_artifact.helper.SSCAppVersionArtifactDescriptor;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.ArtifactDownloadById.CMD_NAME)
public class SSCAppVersionArtifactDownloadByIdCommand extends AbstractSSCAppVersionArtifactOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.ArtifactDownloadById outputHelper;
    @Mixin private SSCAppVersionArtifactDownloadOptions downloadOptions;
    @Mixin private SSCAppVersionArtifactResolverMixin.PositionalParameter artifactResolver;
    
    @Override
    public JsonNode getJsonNode() {
        var unirest = getUnirestInstance();
        SSCAppVersionArtifactDescriptor descriptor = artifactResolver.getArtifactDescriptor(unirest);
        String destination = downloadOptions.getDestination();
        destination = destination != null ? destination : String.format("./artifact_%s.fpr", descriptor.getId());
        SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_ARTIFACT(descriptor.getId(), downloadOptions.isIncludeSources()),
                destination,
                ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);
        return descriptor.asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "ARTIFACT_DOWNLOADED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
