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
package com.fortify.cli.ssc.appversion.cli.cmd;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;
import com.fortify.cli.ssc.access_control.cli.mixin.SSCAppVersionUserMixin;
import com.fortify.cli.ssc.access_control.helper.SSCAppVersionUserUpdateBuilder;
import com.fortify.cli.ssc.app.helper.SSCAppDescriptor;
import com.fortify.cli.ssc.app.helper.SSCAppHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppAndVersionNameResolverMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionCopyFromMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppAndVersionNameDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionCreateCopyFromBuilder;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.attribute.cli.mixin.SSCAttributeUpdateMixin;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeUpdateBuilder;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueTemplateDescriptor;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCAppVersionCreateCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mixin private SSCAppAndVersionNameResolverMixin.PositionalParameter sscAppAndVersionNameResolver;
    @Mixin private SSCIssueTemplateResolverMixin.OptionalOption issueTemplateResolver;
    @Mixin private SSCAttributeUpdateMixin.OptionalAttrOption attrUpdateMixin;
    @Mixin private SSCAppVersionUserMixin.OptionalUserAddOption userAddMixin;
    @Option(names={"--description","-d"}, required = false)
    private String description;
    @Option(names={"--active"}, required = false, defaultValue="true", arity="1")
    private boolean active;
    @Mixin private SSCAppVersionCopyFromMixin copyFromMixin;
    @Option(names={"--auto-required-attrs"}, required = false)
    private boolean autoRequiredAttrs = false;
    @Option(names={"--skip-if-exists"}, required = false)
    private boolean skipIfExists = false;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if ( skipIfExists ) {
            SSCAppVersionDescriptor descriptor = SSCAppVersionHelper.getOptionalAppVersionFromAppAndVersionName(unirest, sscAppAndVersionNameResolver.getAppAndVersionNameDescriptor());
            if ( descriptor!=null ) { return descriptor.asObjectNode().put(IActionCommandResultSupplier.actionFieldName, "SKIPPED_EXISTING"); }
        }
        SSCAttributeUpdateBuilder attrUpdateBuilder = getAttrUpdateBuilder(unirest);
        SSCAppVersionUserUpdateBuilder authUpdateBuilder = getAuthUpdateBuilder(unirest);
        SSCAppVersionCreateCopyFromBuilder copyFromBuilder = getCopyFromBuilder(unirest);

        SSCAppVersionDescriptor descriptor = createUncommittedAppVersion(unirest);
        SSCBulkResponse bulkResponse = new SSCBulkRequestBuilder()
            .request("attrUpdate", attrUpdateBuilder.buildRequest(descriptor.getVersionId()))
            .request("userUpdate", authUpdateBuilder.buildRequest(descriptor.getVersionId()))
            .request("copyFrom", copyFromBuilder.buildCopyFromPartialRequest(descriptor.getVersionId()))
            .request("commit", getCommitRequest(unirest, descriptor))
            .request("copyState", copyFromBuilder.buildCopyStateRequest(descriptor.getVersionId()))
            .request("updatedVersion", unirest.get(SSCUrls.PROJECT_VERSION(descriptor.getVersionId())))
            .execute(unirest);
        return bulkResponse.body("updatedVersion");
    }

    @Override
    public JsonNode transformRecord(JsonNode input) {
        return SSCAppVersionHelper.renameFields(input);
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    private final SSCAppVersionCreateCopyFromBuilder getCopyFromBuilder(UnirestInstance unirest) {
         SSCAppVersionCreateCopyFromBuilder builder = new SSCAppVersionCreateCopyFromBuilder(unirest);
         if(copyFromMixin.isCopyRequested()) {
             builder .setCopyRequested(true)
                     .setCopyFrom(SSCAppVersionHelper.getRequiredAppVersion(unirest, copyFromMixin.getAppVersionNameOrId(), sscAppAndVersionNameResolver.getDelimiter()))
                     .setCopyOptions(copyFromMixin.getCopyOptions());
         }

        return builder;
    }

    private final SSCAppVersionUserUpdateBuilder getAuthUpdateBuilder(UnirestInstance unirest) {
        return new SSCAppVersionUserUpdateBuilder(unirest)
                .add(false, userAddMixin.getAuthEntitySpecs());
    }

    private final SSCAttributeUpdateBuilder getAttrUpdateBuilder(UnirestInstance unirest) {
        Map<String, String> attributes = attrUpdateMixin.getAttributes();
        return new SSCAttributeUpdateBuilder(unirest)
                .add(attributes)
                .addRequiredAttrs(autoRequiredAttrs)
                .checkRequiredAttrs(true)
                .prepareAndCheckRequest();
    }

    private SSCAppVersionDescriptor createUncommittedAppVersion(UnirestInstance unirest) {
        SSCIssueTemplateDescriptor issueTemplateDescriptor = issueTemplateResolver.getIssueTemplateDescriptorOrDefault(unirest);
        SSCAppAndVersionNameDescriptor appAndVersionNameDescriptor = sscAppAndVersionNameResolver.getAppAndVersionNameDescriptor();

        if ( issueTemplateDescriptor==null ) {
            throw new IllegalArgumentException("--issue-template is required, as no default template is configured on SSC");
        }

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
}
