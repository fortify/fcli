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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc.entity.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class SSCAppVersionDeleteCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier, IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @CommandLine.Mixin private SSCAppVersionResolverMixin.PositionalParameter appVersionResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCAppVersionDescriptor descriptor = appVersionResolver.getAppVersionDescriptor(unirest, "id,name,project,createdBy");
        unirest.delete(SSCUrls.PROJECT_VERSION(descriptor.getVersionId())).asObject(JsonNode.class).getBody();
        return descriptor.asObjectNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
        return SSCAppVersionHelper.renameFields(input);
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
