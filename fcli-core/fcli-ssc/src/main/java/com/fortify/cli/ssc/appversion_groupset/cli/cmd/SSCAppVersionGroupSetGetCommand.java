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
package com.fortify.cli.ssc.appversion_groupset.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.*;
import com.fortify.cli.ssc._common.output.cli.cmd.*;
import com.fortify.cli.ssc.appversion.cli.mixin.*;
import com.fortify.cli.ssc.appversion_groupset.cli.mixin.*;
import kong.unirest.*;
import lombok.*;
import picocli.CommandLine.*;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCAppVersionGroupSetGetCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin SSCAppVersionGroupSetResolverMixin.PositionalParameterSingle groupSetResolver;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return groupSetResolver.getGroupSetDescriptor(unirest, parentResolver.getAppVersionId(unirest)).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
