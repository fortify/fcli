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
package com.fortify.cli.ssc.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.ssc.app.cli.mixin.SSCAppResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.Delete.CMD_NAME)
public class SSCAppDeleteCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Delete outputHelper; 
    @Mixin private SSCAppResolverMixin.PositionalParameter appResolver;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        requireConfirmation.checkConfirmed();
        JsonNode versions = getAppVersions(unirest);
        versions.forEach(v->deleteAppVersion(unirest, (ObjectNode)v));
        return versions;
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionHelper.renameFields(record);
    }
    
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }

    private JsonNode getAppVersions(UnirestInstance unirest) {
        return unirest.get(SSCUrls.PROJECT_VERSIONS_LIST(appResolver.getAppId(unirest)))
                .queryString("limit", "-1")
                .queryString("fields", "id,name,project,createdBy")
                .asObject(JsonNode.class).getBody().get("data");
    }
    
    private void deleteAppVersion(UnirestInstance unirest, ObjectNode version) {
        unirest.delete(SSCUrls.PROJECT_VERSION(version.get("id").asText())).asObject(JsonNode.class).getBody();
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
