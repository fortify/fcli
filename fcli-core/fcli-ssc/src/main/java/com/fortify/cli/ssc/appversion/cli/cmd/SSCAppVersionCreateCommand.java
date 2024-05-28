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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

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
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionCopyFromMixin.SSCAppVersionCopyFromDescriptor;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionCopyFromMixin.SSCAppVersionCopyOption;
import com.fortify.cli.ssc.appversion.helper.SSCAppAndVersionNameDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.attribute.cli.mixin.SSCAttributeUpdateMixin;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeUpdateBuilder;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueTemplateHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCAppVersionCreateCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private SSCAppAndVersionNameResolverMixin.PositionalParameter sscAppAndVersionNameResolver;
    @Mixin private SSCIssueTemplateResolverMixin.OptionalOption issueTemplateResolver;
    @Mixin private SSCAttributeUpdateMixin.OptionalAttrOption attrUpdateMixin;
    @Mixin private SSCAppVersionUserMixin.OptionalUserAddOption userAddMixin;
    @Mixin private SSCAppVersionCopyFromMixin copyFromMixin;
    @Option(names={"--description","-d"}, required = false)
    private String description;
    @Option(names={"--active"}, required = false, defaultValue="true", arity="1")
    private boolean active;
    @Option(names={"--auto-required-attrs"}, required = false)
    private boolean autoRequiredAttrs = false;
    @Option(names={"--skip-if-exists"}, required = false)
    private boolean skipIfExists = false;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if ( skipIfExists ) {
            var existingDescriptor = SSCAppVersionHelper.getOptionalAppVersionFromAppAndVersionName(unirest, sscAppAndVersionNameResolver.getAppAndVersionNameDescriptor());
            if ( existingDescriptor!=null ) { return existingDescriptor.asObjectNode().put(IActionCommandResultSupplier.actionFieldName, "SKIPPED_EXISTING"); }
        }
        var copyFromDescriptor = copyFromMixin.getCopyFromDescriptor(unirest);
        var attrUpdateBuilder = getAttrUpdateBuilder(unirest, copyFromDescriptor);
        var authUpdateBuilder = getAuthUpdateBuilder(unirest, copyFromDescriptor);
        
        var descriptor = createUncommittedAppVersion(unirest, copyFromDescriptor);

        SSCBulkResponse bulkResponse = new SSCBulkRequestBuilder()
            .request("attrUpdate", attrUpdateBuilder.buildRequest(descriptor.getVersionId()))
            .request("userUpdate", authUpdateBuilder.buildRequest(descriptor.getVersionId()))
            .request("copyFrom", buildCopyFromPartialRequest(unirest, descriptor, copyFromDescriptor))
            .request("commit", getCommitRequest(unirest, descriptor))
            .request("copyState", buildCopyStateRequest(unirest, descriptor, copyFromDescriptor))
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

    private final SSCAppVersionUserUpdateBuilder getAuthUpdateBuilder(UnirestInstance unirest, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        return new SSCAppVersionUserUpdateBuilder(unirest)
                .add(false, getUsersFromSource(unirest, copyFromDescriptor))
                .add(false, userAddMixin.getAuthEntitySpecs());
    }
    
    private Set<String> getUsersFromSource(UnirestInstance unirest, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        if ( copyFromDescriptor.isCopyRequested() && copyFromDescriptor.getCopyOptions().contains(SSCAppVersionCopyOption.Users) ) {
            return getUsersSet(unirest, copyFromDescriptor.getAppVersionDescriptor());
        }
        return null;
    }
    
    public static final Set<String> getUsersSet(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        Set<String> result = new LinkedHashSet<>();
        var users = SSCAppVersionHelper.getUsers(unirest, descriptor);
        for (JsonNode user : users) {
            result.add(user.get("id").asText());
        }
        return result;
    }

    private final SSCAttributeUpdateBuilder getAttrUpdateBuilder(UnirestInstance unirest, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        return new SSCAttributeUpdateBuilder(unirest)
                .add(getAttributesFromSource(unirest, copyFromDescriptor))
                .add(attrUpdateMixin.getAttributes())
                .addRequiredAttrs(autoRequiredAttrs)
                .checkRequiredAttrs(true)
                .prepareAndCheckRequest();
    }

    private Map<String, String> getAttributesFromSource(UnirestInstance unirest, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        if ( copyFromDescriptor.isCopyRequested() && copyFromDescriptor.getCopyOptions().contains(SSCAppVersionCopyOption.Attributes) ) {
            return getAttributesMap(unirest, copyFromDescriptor.getAppVersionDescriptor());
        }
        return null;
    }
    
    public static final Map<String,String> getAttributesMap(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        var result = new LinkedHashMap<String,String>();
        var attributes = SSCAppVersionHelper.getAttributes(unirest, descriptor);
        for (JsonNode attr : attributes) {
            List<String> values = new ArrayList<>();
            for (JsonNode value: attr.get("values")) {
                values.add(value.get("guid").textValue());
            }
            result.put(attr.get("attributeDefinitionId").toString(), String.join(";", values));
        }
        return result;
    }

    private SSCAppVersionDescriptor createUncommittedAppVersion(UnirestInstance unirest, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        String issueTemplateId = getIssueTemplateId(unirest, copyFromDescriptor);
        SSCAppAndVersionNameDescriptor appAndVersionNameDescriptor = sscAppAndVersionNameResolver.getAppAndVersionNameDescriptor();
        var description = this.description;
        if ( StringUtils.isBlank(description) && copyFromDescriptor.isCopyRequested() ) {
            description = String.format("Copied from "+copyFromDescriptor.getAppVersionDescriptor().getAppAndVersionName());
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", appAndVersionNameDescriptor.getVersionName())
            .put("description", description==null ? "" : description)
            .put("active", active)
            .put("committed", false)
            .put("issueTemplateId", issueTemplateId)
            .set("project", getProjectNode(unirest, appAndVersionNameDescriptor.getAppName(), issueTemplateId));
        JsonNode response = unirest.post(SSCUrls.PROJECT_VERSIONS).body(body).asObject(JsonNode.class).getBody().get("data");
        return JsonHelper.treeToValue(response, SSCAppVersionDescriptor.class);
    }

    private String getIssueTemplateId(UnirestInstance unirest, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        var issueTemplateNameOrId = issueTemplateResolver.getIssueTemplateNameOrId();
        if ( StringUtils.isBlank(issueTemplateNameOrId) && copyFromDescriptor.isCopyRequested() ) {
            issueTemplateNameOrId = copyFromDescriptor.getAppVersionDescriptor().getIssueTemplateId();
        }
        var issueTemplateDescriptor = new SSCIssueTemplateHelper(unirest).getIssueTemplateDescriptorOrDefault(issueTemplateNameOrId);
        if ( issueTemplateDescriptor==null ) {
            throw new IllegalArgumentException("--issue-template is required, as no default template is configured on SSC");
        }
        return issueTemplateDescriptor.getId();
    }

    private JsonNode getProjectNode(UnirestInstance unirest, String appName, String issueTemplateId) {
        SSCAppDescriptor appDescriptor = SSCAppHelper.getApp(unirest, appName, false, "id");
        if ( appDescriptor!=null ) {
            return appDescriptor.asJsonNode();
        } else {
            ObjectNode appNode = new ObjectMapper().createObjectNode();
            appNode.put("name", appName);
            appNode.put("issueTemplateId", issueTemplateId);
            return appNode;
        }
    }

    private final HttpRequest<?> getCommitRequest(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        ObjectNode body = objectMapper.createObjectNode().put("committed", true);
        return unirest.put(SSCUrls.PROJECT_VERSION(descriptor.getVersionId())).body(body);
    }
    
    private HttpRequest<?> buildCopyFromPartialRequest(UnirestInstance unirest, SSCAppVersionDescriptor copyTo, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        if ( !copyFromDescriptor.isCopyRequested() ) { return null; }
        var properties = copyFromDescriptor.getCopyOptions().stream()
                .map(SSCAppVersionCopyOption::getCopyFromPartialProperty)
                .filter(Objects::nonNull)
                .toList();
        if ( properties.isEmpty() ) { return null; }
        var body = buildCopyFromAppVersionIdsBody(copyTo, copyFromDescriptor);
        properties.forEach(p->body.put(p, true));
        return unirest.post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_FROM_PARTIAL)
                    .body(body);
    }

    private HttpRequest<?> buildCopyStateRequest(UnirestInstance unirest, SSCAppVersionDescriptor copyTo, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        if ( !copyFromDescriptor.isCopyRequested() 
                || !copyFromDescriptor.getCopyOptions().contains(SSCAppVersionCopyOption.State)) { 
            return null; 
        }
        return unirest.post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_CURRENT_STATE)
                    .body(buildCopyFromAppVersionIdsBody(copyTo, copyFromDescriptor));
    }
    
    private ObjectNode buildCopyFromAppVersionIdsBody(SSCAppVersionDescriptor copyTo, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        var copyFrom = copyFromDescriptor.getAppVersionDescriptor();
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("projectVersionId", copyTo.getVersionId())
                .put("previousProjectVersionId", copyFrom.getVersionId());
    }

}
