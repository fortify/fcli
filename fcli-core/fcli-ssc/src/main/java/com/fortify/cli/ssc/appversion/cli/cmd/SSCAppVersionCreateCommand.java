/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
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
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.ssc.bulk.SSCBulkRequestBuilder.SSCBulkResponse;
import com.fortify.cli.ssc.access_control.cli.mixin.SSCAppVersionUserMixin;
import com.fortify.cli.ssc.access_control.helper.SSCAppVersionUserUpdateBuilder;
import com.fortify.cli.ssc.app.helper.SSCAppDescriptor;
import com.fortify.cli.ssc.app.helper.SSCAppHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppAndVersionNameResolverMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionCopyFromMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionCopyFromMixin.SSCAppVersionCopyFromDescriptor;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionCopyFromMixin.SSCAppVersionCopyOption;
import com.fortify.cli.ssc.appversion.helper.SSCAppAndVersionNameDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionCustomTagUpdater;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.attribute.cli.mixin.SSCAttributeUpdateMixin;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeUpdateBuilder;
import com.fortify.cli.ssc.custom_tag.cli.mixin.SSCCustomTagAddRemoveMixin;
import com.fortify.cli.ssc.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateHelper;

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
    @Mixin private SSCCustomTagAddRemoveMixin.OptionalTagAddOption tagAddMixin;
    @Mixin private SSCCustomTagAddRemoveMixin.OptionalTagRemoveOption tagRemoveMixin;
    @Mixin private CommonOptionMixins.SkipIfExistsOption skipIfExistsOption;
    @Mixin private CommonOptionMixins.AutoRequiredAttrsOption autoRequiredAttrsOption;

    @Option(names={"--description","-d"}, required = false)
    private String description;
    @Option(names={"--active"}, required = false, defaultValue="true", arity="1")
    private boolean active;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if ( skipIfExistsOption.isSkipIfExists() ) {
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
        JsonNode updatedVersion = bulkResponse.body("updatedVersion");
        // Perform custom tag update AFTER commit/copy to ensure we see final server-assigned tags
        HttpRequest<?> customTagUpdate = buildPostCommitCustomTagUpdateRequest(unirest, copyFromDescriptor, descriptor);
        if ( customTagUpdate!=null ) {
            customTagUpdate.asObject(JsonNode.class).getBody(); // execute, no need to interpret body
            updatedVersion = unirest.get(SSCUrls.PROJECT_VERSION(descriptor.getVersionId())).asObject(JsonNode.class).getBody().get("data");
        }
        return updatedVersion;
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
        if ( copyFromDescriptor.isCopyRequested() && copyFromDescriptor.getCopyOptions().contains(SSCAppVersionCopyOption.users) ) {
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
                .addRequiredAttrs(autoRequiredAttrsOption.isAutoRequiredAttrs())
                .checkRequiredAttrs(true)
                .prepareAndCheckRequest();
    }

    private Map<String, String> getAttributesFromSource(UnirestInstance unirest, SSCAppVersionCopyFromDescriptor copyFromDescriptor) {
        if ( copyFromDescriptor.isCopyRequested() && copyFromDescriptor.getCopyOptions().contains(SSCAppVersionCopyOption.attributes) ) {
            return getAttributesMap(unirest, copyFromDescriptor.getAppVersionDescriptor());
        }
        return null;
    }
    
    public static final Map<String,String> getAttributesMap(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        var result = new LinkedHashMap<String,String>();
        var attributes = SSCAppVersionHelper.getAttributes(unirest, descriptor);
        for (JsonNode attr : attributes) {
            List<String> values = getAttributeValues(attr);
            result.put(attr.get("attributeDefinitionId").toString(), String.join(";", values));
        }
        return result;
    }

    private static List<String> getAttributeValues(JsonNode attr) {
        List<String> values = new ArrayList<>();
        var value = attr.get("value");
        if ( value!=null && !value.isNull() ) {
            values.add(value.asText());
        } else {
            for (JsonNode valuesElt: attr.get("values")) {
                values.add(valuesElt.get("guid").textValue());
            }
        }
        return values;
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
        var issueTemplateDescriptor = new SSCIssueTemplateHelper(unirest).getIssueTemplateDescriptorOrDefaultorInUse(issueTemplateNameOrId);
        if ( issueTemplateDescriptor==null ) {
            throw new FcliSimpleException("--issue-template is required, as no default template or single In-Use template is configured on SSC");
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
                || !copyFromDescriptor.getCopyOptions().contains(SSCAppVersionCopyOption.state)) { 
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

    private HttpRequest<?> buildPostCommitCustomTagUpdateRequest(UnirestInstance unirest, SSCAppVersionCopyFromDescriptor copyFromDescriptor, SSCAppVersionDescriptor descriptor) {
        return new SSCAppVersionCustomTagUpdater(unirest)
                .buildRequest(descriptor.getVersionId(), tagAddMixin.getTagSpecs(), tagRemoveMixin.getTagSpecs());
    }
}