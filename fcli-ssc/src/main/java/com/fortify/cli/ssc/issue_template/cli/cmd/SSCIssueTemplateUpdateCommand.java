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
package com.fortify.cli.ssc.issue_template.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = SSCOutputHelperMixins.Update.CMD_NAME)
public class SSCIssueTemplateUpdateCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Update outputHelper; 
    @Mixin private SSCIssueTemplateResolverMixin.PositionalParameterSingle issueTemplateResolver;
    @Option(names={"--name","-n"}, required = false)
    private String name;
    @Option(names={"--description","-d"}, required = false)
    private String description;
    @Option(names={"--set-as-default"})
    private boolean setAsDefault;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCIssueTemplateDescriptor descriptor = issueTemplateResolver.getIssueTemplateDescriptor(unirest);
        ObjectNode updateData = (ObjectNode)descriptor.asJsonNode();
        if ( StringUtils.isNotBlank(name) ) { updateData.put("name", name); }
        if ( StringUtils.isNotBlank(description) ) { updateData.put("description", description); }
        if ( setAsDefault ) { updateData.put("defaultTemplate", true); }
        unirest.put(SSCUrls.ISSUE_TEMPLATE(descriptor.getId()))
            .body(updateData).asObject(JsonNode.class).getBody();
        return new SSCIssueTemplateHelper(unirest).getDescriptorByNameOrId(descriptor.getId(), true).asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
