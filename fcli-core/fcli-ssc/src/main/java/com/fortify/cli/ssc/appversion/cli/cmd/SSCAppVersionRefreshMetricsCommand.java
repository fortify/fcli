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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionBulkEmbedMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.AppVersionRefreshMettrics.CMD_NAME)
public class SSCAppVersionRefreshMetricsCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.AppVersionRefreshMettrics outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.PositionalParameter appVersionResolver;
    @Mixin private SSCAppVersionBulkEmbedMixin bulkEmbedMixin;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var descriptor = appVersionResolver.getAppVersionDescriptor(unirest);
        return descriptor.asObjectNode()
                .put(IActionCommandResultSupplier.actionFieldName, SSCAppVersionHelper.refreshMetrics(unirest, descriptor));
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionHelper.renameFields(record);
    }
    
    @Override
    public String getActionCommandResult() {
        return "REFRESH_REQUESTED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
