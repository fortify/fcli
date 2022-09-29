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
package com.fortify.cli.ssc.appversion.cli.cmd;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.ssc.app.helper.SSCAppDescriptor;
import com.fortify.cli.ssc.app.helper.SSCAppHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin.SSCAppVersionDelimiterMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin.SSCAppVersionDelimiterMixin.SSCAppAndVersionNameDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.appversion_attribute.cli.mixin.SSCAppVersionAttributeUpdateMixin;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAppVersionAttributeUpdateBuilder;
import com.fortify.cli.ssc.appversion_user.cli.mixin.SSCAppVersionAuthEntityMixin;
import com.fortify.cli.ssc.appversion_user.helper.SSCAppVersionAuthEntitiesUpdateBuilder;
import com.fortify.cli.ssc.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = "create")
public class SSCAppVersionCreateCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mixin private OutputMixin outputMixin;
    @Parameters(index = "0", arity = "1", descriptionKey = "appVersionName") private String appAndVersionName;
    @Mixin private SSCAppVersionDelimiterMixin delimiterMixin;
    @Mixin private SSCIssueTemplateResolverMixin.OptionalFilterSetOption issueTemplateResolver;
    @Mixin private SSCAppVersionAttributeUpdateMixin.OptionalAttrOption attrUpdateMixin;
    @Mixin private SSCAppVersionAuthEntityMixin.OptionalUserAddOption userAddMixin;
    @Option(names={"--description","-d"}, required = false)
    private String description;
    @Option(names={"--active"}, required = false, defaultValue="true", arity="1")
    private boolean active;
    @Option(names={"--auto-required-attrs"}, required = false)
    private boolean autoRequiredAttrs = false;
    

    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        SSCAppVersionAttributeUpdateBuilder attrUpdateBuilder = getAttrUpdateBuilder(unirest);
        SSCAppVersionAuthEntitiesUpdateBuilder authUpdateBuilder = getAuthUpdateBuilder(unirest);
        
        SSCAppVersionDescriptor descriptor = createUncommittedAppVersion(unirest);
        SSCBulkResponse bulkResponse = new SSCBulkRequestBuilder()
            .request("attrUpdate", attrUpdateBuilder.buildRequest(descriptor.getVersionId()))
            .request("userUpdate", authUpdateBuilder.buildRequest(descriptor.getVersionId()))
            .request("commit", getCommitRequest(unirest, descriptor))
            .request("updatedVersion", unirest.get(SSCUrls.PROJECT_VERSION(descriptor.getVersionId())))
            .execute(unirest);
        // TODO We probably also want to add attribute and user data to the detailed output
        outputMixin.write(bulkResponse.body("updatedVersion"));
        return null;
    }
    
    private final SSCAppVersionAuthEntitiesUpdateBuilder getAuthUpdateBuilder(UnirestInstance unirest) {
        return new SSCAppVersionAuthEntitiesUpdateBuilder(unirest)
                .add(false, userAddMixin.getAuthEntitySpecs());
    }
    
    private final SSCAppVersionAttributeUpdateBuilder getAttrUpdateBuilder(UnirestInstance unirest) {
        Map<String, String> attributes = attrUpdateMixin.getAttributes();
        return new SSCAppVersionAttributeUpdateBuilder(unirest)
                .add(attributes)
                .addRequiredAttrs(autoRequiredAttrs)
                .checkRequiredAttrs(true)
                .prepareAndCheckRequest();
    }

    private SSCAppVersionDescriptor createUncommittedAppVersion(UnirestInstance unirest) {
        SSCIssueTemplateDescriptor issueTemplateDescriptor = issueTemplateResolver.getIssueTemplateDescriptorOrDefault(unirest);
        SSCAppAndVersionNameDescriptor appAndVersionNameDescriptor = delimiterMixin.getAppAndVersionNameDescriptor(appAndVersionName);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", appAndVersionNameDescriptor.getVersionName())
            .put("description", description==null ? "" : description)
            .put("active", active)
            .put("committed", false)
            .put("issueTemplateId", issueTemplateDescriptor.getId())
            .set("project", getProjectNode(unirest, appAndVersionNameDescriptor.getAppName(), issueTemplateDescriptor));
        JsonNode response = unirest.post(SSCUrls.PROJECT_VERSIONS).body(body).asObject(JsonNode.class).getBody().get("data");
        return JsonHelper.treeToValue(response, SSCAppVersionDescriptor.class);
    }
    
    private JsonNode getProjectNode(UnirestInstance unirest, String appName, SSCIssueTemplateDescriptor issueTemplateDescriptor) {
        SSCAppDescriptor appDescriptor = SSCAppHelper.getApp(unirest, appName, false, "id");
        if ( appDescriptor!=null ) {
            return appDescriptor.asJsonNode();
        } else {
            ObjectNode appNode = new ObjectMapper().createObjectNode();
            appNode.put("name", appName);
            appNode.put("issueTemplateId", issueTemplateDescriptor.getId());
            return appNode;
        }
    }
    
    private final HttpRequest<?> getCommitRequest(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        ObjectNode body = objectMapper.createObjectNode().put("committed", true);
        return unirest.put(SSCUrls.PROJECT_VERSION(descriptor.getVersionId())).body(body);
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputConfigHelper.details().recordTransformer(SSCAppVersionHelper::renameFields);
    }
}
