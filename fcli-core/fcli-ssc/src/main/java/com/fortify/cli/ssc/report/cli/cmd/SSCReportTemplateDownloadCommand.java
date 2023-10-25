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
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;
import com.fortify.cli.ssc.report.cli.mixin.SSCReportTemplateResolverMixin;
import com.fortify.cli.ssc.report.helper.SSCReportTemplateDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.DownloadTemplate.CMD_NAME) @CommandGroup("template")
public class SSCReportTemplateDownloadCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DownloadTemplate outputHelper;
    @Mixin private SSCReportTemplateResolverMixin.PositionalParameterSingle reportTemplateResolver;
    @Mixin private CommonOptionMixins.OptionalFile fileMixin;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCReportTemplateDescriptor descriptor = reportTemplateResolver.getReportTemplateDescriptor(unirest);
        var destination = fileMixin.getFile();
        if ( destination==null ) {
            destination = new File(String.format("./%s", descriptor.getFileName()));
        }
        SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_REPORT_DEFINITION_TEMPLATE(descriptor.getId()),
                destination,
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
