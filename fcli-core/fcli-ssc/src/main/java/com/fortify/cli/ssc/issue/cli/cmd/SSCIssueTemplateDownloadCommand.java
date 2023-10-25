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

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueTemplateDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.DownloadTemplate.CMD_NAME) @CommandGroup("template")
public class SSCIssueTemplateDownloadCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DownloadTemplate outputHelper;
    @Mixin private CommonOptionMixins.OptionalFile fileMixin;

    @CommandLine.Mixin
    private SSCIssueTemplateResolverMixin.PositionalParameterSingle issueTemplateResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCIssueTemplateDescriptor descriptor = issueTemplateResolver.getIssueTemplateDescriptor(unirest);
        String issueTemplateId = descriptor.getId();
        File destination = fileMixin.getFile();
        if (destination==null ) {
            destination = new File(String.format("./%s", descriptor.getOriginalFileName()));
        }
        SSCFileTransferHelper.download(
                unirest,
                String.format("/download/projectTemplateDownload.html?guid=%s", issueTemplateId),
                destination,
                ISSCAddDownloadTokenFunction.QUERYSTRING_MAT
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
