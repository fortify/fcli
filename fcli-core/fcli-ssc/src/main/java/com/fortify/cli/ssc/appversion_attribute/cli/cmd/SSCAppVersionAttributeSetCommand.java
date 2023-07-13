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
package com.fortify.cli.ssc.appversion_attribute.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion_attribute.cli.mixin.SSCAppVersionAttributeUpdateMixin;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAppVersionAttributeListHelper;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAppVersionAttributeUpdateBuilder;
import com.fortify.cli.ssc.attribute_definition.helper.SSCAttributeDefinitionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Set.CMD_NAME)
public class SSCAppVersionAttributeSetCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Set outputHelper;
    @Mixin private SSCAppVersionAttributeUpdateMixin.RequiredPositionalParameter attrUpdateMixin;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCAttributeDefinitionHelper attrDefHelper = new SSCAttributeDefinitionHelper(unirest);
        SSCAppVersionAttributeUpdateBuilder attrUpdateHelper = new SSCAppVersionAttributeUpdateBuilder(unirest, attrDefHelper)
                .add(attrUpdateMixin.getAttributes());
        String applicationVersionId = parentResolver.getAppVersionId(unirest);
        
        return new SSCAppVersionAttributeListHelper()
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
