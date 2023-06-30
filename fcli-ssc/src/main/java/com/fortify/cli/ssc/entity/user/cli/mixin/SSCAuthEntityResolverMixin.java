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
package com.fortify.cli.ssc.entity.user.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.ssc.entity.user.helper.SSCAuthEntitiesHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAuthEntityResolverMixin {
    private static abstract class AbstractSSCAuthEntityResolverMixin {
        public abstract String getAuthEntitySpec();
        
        public JsonNode getAuthEntityJsonNode(UnirestInstance unirest) {
            return new SSCAuthEntitiesHelper(unirest).getAuthEntities(false, true, getAuthEntitySpec());
        }
    }
    
    public static class RequiredOption extends AbstractSSCAuthEntityResolverMixin {
        @Option(names="--user", required = true)
        @Getter private String authEntitySpec;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCAuthEntityResolverMixin {
        @Parameters(index = "0", arity = "1")
        @Getter private String authEntitySpec;
    }
}
