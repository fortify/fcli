package com.fortify.cli.ssc.issue_template.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.bulk.SSCBulkRequestBuilder;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public abstract class AbstractSSCIssueTemplateCreateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Option(names={"--name","-n"}, required = true) protected String issueTemplateName;
    @Mixin protected CommonOptionMixins.RequiredFile fileMixin;
    @Option(names={"--description","-d"}, required = false, defaultValue = "")
    protected String description;
    @Option(names={"--set-as-default"})
    protected boolean setAsDefault;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        JsonNode body = unirest.post(SSCUrls.ISSUE_TEMPLATES)
                .queryString("name", issueTemplateName)
                .queryString("description", description)
                .queryString("confirmIgnoreCustomTagUpdates", "true")
                .multiPartContent()
                .field("file", fileMixin.getFile())
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
