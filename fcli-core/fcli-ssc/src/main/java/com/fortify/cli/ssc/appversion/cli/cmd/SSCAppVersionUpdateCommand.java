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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.appversion_user.cli.mixin.SSCAppVersionUserMixin;
import com.fortify.cli.ssc.appversion_user.helper.SSCAppVersionUserUpdateBuilder;
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

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class SSCAppVersionUpdateCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.PositionalParameter appVersionResolver;
    @Mixin private SSCIssueTemplateResolverMixin.OptionalOption issueTemplateResolver;
    @Mixin private SSCAttributeUpdateMixin.OptionalAttrOption attrUpdateMixin;
    @Mixin private SSCAppVersionUserMixin.OptionalUserAddOption userAddMixin;
    @Mixin private SSCAppVersionUserMixin.OptionalUserRemoveOption userDelMixin;
    @Option(names={"--name","-n"}, required = false)
    private String name;
    @Option(names={"--description","-d"}, required = false)
    private String description;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCAppVersionDescriptor descriptor = appVersionResolver.getAppVersionDescriptor(unirest);
        SSCBulkResponse bulkResponse = new SSCBulkRequestBuilder()
            .request("versionUpdate", getAppVersionUpdateRequest(unirest, descriptor))
            .request("attrUpdate", getAttrUpdateRequest(unirest, descriptor))
            .request("userUpdate", getUserUpdateRequest(unirest, descriptor))
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
        return "UPDATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private final HttpRequest<?> getUserUpdateRequest(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        return new SSCAppVersionUserUpdateBuilder(unirest)
                .add(false, userAddMixin.getAuthEntitySpecs())
                .remove(false, userDelMixin.getAuthEntitySpecs())
                .buildRequest(descriptor.getVersionId());
    }
    
    private final HttpRequest<?> getAttrUpdateRequest(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        Map<String, String> attributes = attrUpdateMixin.getAttributes();
        if ( attributes==null || attributes.isEmpty() ) { return null; }
        SSCAttributeUpdateBuilder attrUpdateHelper = new SSCAttributeUpdateBuilder(unirest).add(attributes);
        return attrUpdateHelper.buildRequest(descriptor.getVersionId());
    }
    
    private final HttpRequest<?> getAppVersionUpdateRequest(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        ObjectNode updateData = (ObjectNode)descriptor.asJsonNode();
        boolean hasUpdate = optionalUpdate(updateData, "name", getUnqualifiedVersionName(name, descriptor));
        hasUpdate |= optionalUpdate(updateData, "description", description);
        hasUpdate |= optionalUpdate(updateData, issueTemplateResolver.getIssueTemplateDescriptor(unirest));
        return hasUpdate 
                ? unirest.put(SSCUrls.PROJECT_VERSION(descriptor.getVersionId())).body(updateData)
                : null;
    }
    
    private boolean optionalUpdate(ObjectNode updateData, String name, String value) {
        if ( StringUtils.isBlank(value) ) { return false; }
        updateData.put(name, value);
        return true;
    }
    
    private boolean optionalUpdate(ObjectNode updateData, SSCIssueTemplateDescriptor descriptor) {
        if ( descriptor==null ) { return false; }
        updateData.put("issueTemplateId", descriptor.getId());
        updateData.put("issueTemplateName", descriptor.getName());
        return true;
    }
    
    private String getUnqualifiedVersionName(String potentialQualifiedName, SSCAppVersionDescriptor descriptor) {
        if ( StringUtils.isBlank(potentialQualifiedName) ) { return null; }
        var delim = appVersionResolver.getDelimiterMixin().getDelimiter();
        var qualifierPrefix = descriptor.getQualifierPrefix(delim);
        var result = !potentialQualifiedName.startsWith(qualifierPrefix)
                ? potentialQualifiedName
                : potentialQualifiedName.substring(qualifierPrefix.length());
        if ( result.contains(delim) ) {
            throw new IllegalArgumentException(String.format("--name option must contain either a plain name or %s<new name>, current: %s", qualifierPrefix, potentialQualifiedName));
        }
        return result;
    }
}
