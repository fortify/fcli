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
package com.fortify.cli.ssc.attribute.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.attribute.cli.mixin.SSCAttributeUpdateMixin;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeListHelper;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeUpdateBuilder;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class SSCAttributeUpdateCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private SSCAttributeUpdateMixin.RequiredAttrOption attrUpdateMixin;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCAttributeDefinitionHelper attrDefHelper = new SSCAttributeDefinitionHelper(unirest);
        SSCAttributeUpdateBuilder attrUpdateHelper = new SSCAttributeUpdateBuilder(unirest, attrDefHelper)
                .add(attrUpdateMixin.getAttributes());
        String applicationVersionId = parentResolver.getAppVersionId(unirest);
        
        return new SSCAttributeListHelper()
                .attributeDefinitionHelper(attrDefHelper)
                .request("attrUpdate", attrUpdateHelper.buildRequest(applicationVersionId))
                .attrIdsToInclude(attrUpdateHelper.getAttributeIds())
                .execute(unirest, applicationVersionId);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
