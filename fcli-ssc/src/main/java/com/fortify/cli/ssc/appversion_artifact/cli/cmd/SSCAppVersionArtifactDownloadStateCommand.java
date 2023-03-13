/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion_artifact.cli.mixin.SSCAppVersionArtifactDownloadOptions;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = SSCOutputHelperMixins.ArtifactDownloadState.CMD_NAME)
// We're outputting an appversion, not an artifact, hence we configure an empty default variable property name
@DefaultVariablePropertyName("") 
public class SSCAppVersionArtifactDownloadStateCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.ArtifactDownloadState outputHelper;
    @Mixin private SSCAppVersionArtifactDownloadOptions downloadOptions;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
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
