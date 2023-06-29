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
package com.fortify.cli.ssc.entity.issue_template.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.entity.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.entity.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddDownloadTokenFunction;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Download.CMD_NAME)
public class SSCIssueTemplateDownloadCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Download outputHelper;
    @CommandLine.Option(names = {"-f", "--dest"}, descriptionKey = "download.destination")
    private String destination;

    @CommandLine.Mixin
    private SSCIssueTemplateResolverMixin.PositionalParameterSingle issueTemplateResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCIssueTemplateDescriptor descriptor = issueTemplateResolver.getIssueTemplateDescriptor(unirest);
        String issueTemplateId = descriptor.getId();
        destination = destination != null ? destination : String.format("./%s", descriptor.getOriginalFileName());
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