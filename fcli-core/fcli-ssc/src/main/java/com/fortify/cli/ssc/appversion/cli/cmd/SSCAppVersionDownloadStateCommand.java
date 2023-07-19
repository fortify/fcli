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
package com.fortify.cli.ssc.appversion.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.artifact.cli.mixin.SSCArtifactDownloadOptions;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.AppVersionDownloadState.CMD_NAME)
public class SSCAppVersionDownloadStateCommand extends AbstractSSCOutputCommand implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.AppVersionDownloadState outputHelper;
    @Mixin private SSCArtifactDownloadOptions downloadOptions;
    @Mixin private SSCAppVersionResolverMixin.PositionalParameter parentResolver;
    
    @Override
    public JsonNode getJsonNode() {
        var unirest = getUnirestInstance();
        SSCAppVersionDescriptor av = parentResolver.getAppVersionDescriptor(unirest);
        File destination = downloadOptions.getDestination().getOutputFile();
        if ( destination==null ) {
            destination = new File(String.format("./%s_%s.fpr", av.getApplicationName(), av.getVersionName()));
        }
        SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), downloadOptions.isIncludeSources()),
                destination,
                ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);
        return av.asJsonNode();
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionHelper.renameFields(record);
    }
    
    @Override
    public String getActionCommandResult() {
        return "STATE_DOWNLOADED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
