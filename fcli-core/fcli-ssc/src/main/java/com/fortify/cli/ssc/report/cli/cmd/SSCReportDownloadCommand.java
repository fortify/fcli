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
package com.fortify.cli.ssc.report.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.SSCFileTransferTokenType;
import com.fortify.cli.ssc.report.cli.mixin.SSCReportResolverMixin;
import com.fortify.cli.ssc.report.helper.SSCReportDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Download.CMD_NAME)
public class SSCReportDownloadCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Download outputHelper;
    @Mixin private SSCReportResolverMixin.PositionalParameterSingle reportResolver;
    @Mixin private CommonOptionMixins.OptionalFile fileMixin;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCReportDescriptor descriptor = reportResolver.getReportDescriptor(unirest);
        var destination = fileMixin.getFile();
        if ( destination==null ) {
            destination = new File(String.format("./%s.%s", descriptor.getName(), descriptor.getFormat().toLowerCase()));
        }
        SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_REPORT(descriptor.getIdString()),
                destination,
                SSCFileTransferTokenType.REPORT_FILE,
                ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN
        );
        return descriptor.asJsonNode();
    }

    @Override
    public String getActionCommandResult() {
        return "DOWNLOADED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
