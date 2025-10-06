package com.fortify.cli.ssc.issue_template.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractSSCIssueTemplateGetCommand extends AbstractSSCJsonNodeOutputCommand {
    @Mixin protected SSCIssueTemplateResolverMixin.PositionalParameterSingle issueTemplateResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return issueTemplateResolver.getIssueTemplateDescriptor(unirest).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
