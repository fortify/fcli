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
package com.fortify.cli.fod.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.app.helper.FoDAppHelper;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class FoDAppGetCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private FoDAppResolverMixin.PositionalParameter appResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return appResolver.getAppDescriptor(unirest).asJsonNode();
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppHelper.transformRecord(record);
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }

}
