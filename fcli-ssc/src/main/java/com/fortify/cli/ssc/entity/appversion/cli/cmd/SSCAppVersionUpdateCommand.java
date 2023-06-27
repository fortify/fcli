/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.appversion.cli.cmd;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.entity.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.entity.appversion_attribute.cli.mixin.SSCAppVersionAttributeUpdateMixin;
import com.fortify.cli.ssc.entity.appversion_attribute.helper.SSCAppVersionAttributeUpdateBuilder;
import com.fortify.cli.ssc.entity.appversion_user.cli.mixin.SSCAppVersionAuthEntityMixin;
import com.fortify.cli.ssc.entity.appversion_user.helper.SSCAppVersionAuthEntitiesUpdateBuilder;
import com.fortify.cli.ssc.entity.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.entity.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;

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
    @Mixin private SSCIssueTemplateResolverMixin.OptionalFilterSetOption issueTemplateResolver;
    @Mixin private SSCAppVersionAttributeUpdateMixin.OptionalAttrOption attrUpdateMixin;
    @Mixin private SSCAppVersionAuthEntityMixin.OptionalUserAddOption userAddMixin;
    @Mixin private SSCAppVersionAuthEntityMixin.OptionalUserDelOption userDelMixin;
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
        return new SSCAppVersionAuthEntitiesUpdateBuilder(unirest)
                .add(false, userAddMixin.getAuthEntitySpecs())
                .remove(false, userDelMixin.getAuthEntitySpecs())
                .buildRequest(descriptor.getVersionId());
    }
    
    private final HttpRequest<?> getAttrUpdateRequest(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        Map<String, String> attributes = attrUpdateMixin.getAttributes();
        if ( attributes==null || attributes.isEmpty() ) { return null; }
        SSCAppVersionAttributeUpdateBuilder attrUpdateHelper = new SSCAppVersionAttributeUpdateBuilder(unirest).add(attributes);
        return attrUpdateHelper.buildRequest(descriptor.getVersionId());
    }
    
    private final HttpRequest<?> getAppVersionUpdateRequest(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        ObjectNode updateData = (ObjectNode)descriptor.asJsonNode();
        boolean hasUpdate = optionalUpdate(updateData, "name", name);
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
}
