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

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = SSCOutputHelperMixins.Create.CMD_NAME)
public class SSCIssueTemplateCreateCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Create outputHelper; 
    @Parameters(index = "0", arity = "1", descriptionKey = "issueTemplateName")
    private String issueTemplateName;
    @Option(names={"--issue-template-file","-f"}, required = true)
    private String fileName;
    @Option(names={"--description","-d"}, required = false, defaultValue = "")
    private String description;
    @Option(names={"--set-as-default"})
    private boolean setAsDefault;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        JsonNode body = unirest.post(SSCUrls.ISSUE_TEMPLATES)
                .queryString("name", issueTemplateName)
                .queryString("description", description)
                .queryString("confirmIgnoreCustomTagUpdates", "true")
                .multiPartContent()
                .field("file", new File(fileName))
                .asObject(JsonNode.class).getBody();
        if ( setAsDefault ) {
            ObjectNode data = (ObjectNode)body.get("data").deepCopy();
            data.put("defaultTemplate", true);
            String url = SSCUrls.ISSUE_TEMPLATE(data.get("id").asText());
			body = new SSCBulkRequestBuilder()
            	.request("update", unirest.put(url).body(data))
            	.request("result", unirest.get(url))
            	.execute(unirest)
            	.body("result");
        }
        return body;
    }
    
    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
