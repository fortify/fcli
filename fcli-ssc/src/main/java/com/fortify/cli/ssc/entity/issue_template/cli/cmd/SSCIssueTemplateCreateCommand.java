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
package com.fortify.cli.ssc.entity.issue_template.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCIssueTemplateCreateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper; 
    @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.issue-template.name")
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
