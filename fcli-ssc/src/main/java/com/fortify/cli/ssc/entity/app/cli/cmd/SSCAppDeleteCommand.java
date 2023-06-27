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
package com.fortify.cli.ssc.entity.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc.entity.app.cli.mixin.SSCAppResolverMixin;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class SSCAppDeleteCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper; 
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
