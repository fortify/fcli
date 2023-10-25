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
package com.fortify.cli.ssc.attribute.cli.mixin;

import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeDefinitionDescriptor;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeDefinitionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Parameters;

public class SSCAttributeDefinitionResolverMixin {
    public static abstract class AbstractSSCAttributeDefinitionResolverMixin {
        public abstract String getAttributeDefinitionNameOrId();

        public SSCAttributeDefinitionDescriptor getAttributeDefinitionDescriptor(UnirestInstance unirest) {
            return new SSCAttributeDefinitionHelper(unirest).getAttributeDefinitionDescriptor(getAttributeDefinitionNameOrId());
        }
    }
    
    public static class PositionalParameterSingle extends AbstractSSCAttributeDefinitionResolverMixin {
        @EnvSuffix("ATTRDEF") @Parameters(index = "0", arity = "1", paramLabel = "[CATEGORY:]ATTR", descriptionKey = "fcli.ssc.attribute-definition.resolver.nameOrId")
        @Getter private String attributeDefinitionNameOrId;
    }
}
