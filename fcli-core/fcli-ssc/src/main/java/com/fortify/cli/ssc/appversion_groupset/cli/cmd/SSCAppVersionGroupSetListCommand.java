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
import com.fortify.cli.ssc._common.rest.*;
import com.fortify.cli.ssc.appversion.cli.mixin.*;
import kong.unirest.*;
import lombok.*;
import picocli.CommandLine.*;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCAppVersionGroupSetListCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return unirest.get(SSCUrls.PROJECT_VERSION_ISSUE_SELECTOR_SET(parentResolver.getAppVersionId(unirest)))
                .asObject(JsonNode.class).getBody()
                .get("data")
                .get("groupBySet");
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
