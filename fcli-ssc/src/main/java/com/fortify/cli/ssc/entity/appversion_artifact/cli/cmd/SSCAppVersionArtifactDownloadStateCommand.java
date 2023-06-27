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
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc.entity.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.entity.appversion_artifact.cli.mixin.SSCAppVersionArtifactDownloadOptions;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.ArtifactDownloadState.CMD_NAME)
// We're outputting an appversion, not an artifact, hence we configure an empty default variable property name
@DefaultVariablePropertyName("") 
public class SSCAppVersionArtifactDownloadStateCommand extends AbstractSSCAppVersionArtifactOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.ArtifactDownloadState outputHelper;
    @Mixin private SSCAppVersionArtifactDownloadOptions downloadOptions;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public JsonNode getJsonNode() {
        var unirest = getUnirestInstance();
        SSCAppVersionDescriptor av = parentResolver.getAppVersionDescriptor(unirest);
        String destination = downloadOptions.getDestination();
        destination = destination != null ? destination : String.format("./%s_%s.fpr", av.getApplicationName(), av.getVersionName());
        SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), downloadOptions.isIncludeSources()),
                destination,
                ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);
        return av.asJsonNode();
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
