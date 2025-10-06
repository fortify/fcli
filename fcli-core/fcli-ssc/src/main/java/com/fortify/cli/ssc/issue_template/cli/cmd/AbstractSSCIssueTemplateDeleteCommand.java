package com.fortify.cli.ssc.issue_template.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateDescriptor;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractSSCIssueTemplateDeleteCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Mixin protected SSCIssueTemplateResolverMixin.PositionalParameterSingle issueTemplateResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCIssueTemplateDescriptor descriptor = issueTemplateResolver.getIssueTemplateDescriptor(unirest);
        unirest.delete(SSCUrls.ISSUE_TEMPLATE(descriptor.getId())).asString().getBody();
        return descriptor.asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
