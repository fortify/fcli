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
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.appversion_attribute.cli.mixin.SSCAppVersionAttributeUpdateMixin;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAppVersionAttributeUpdateBuilder;
import com.fortify.cli.ssc.appversion_user.cli.mixin.SSCAppVersionAuthEntityMixin;
import com.fortify.cli.ssc.appversion_user.helper.SSCAppVersionAuthEntitiesUpdateBuilder;
import com.fortify.cli.ssc.issue_template.cli.mixin.SSCIssueTemplateResolverMixin;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SSCOutputHelperMixins.Update.CMD_NAME)
public class SSCAppVersionUpdateCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformerSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Update outputHelper; 
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
    public UnaryOperator<JsonNode> getRecordTransformer() {
    	return SSCAppVersionHelper::renameFields;
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
