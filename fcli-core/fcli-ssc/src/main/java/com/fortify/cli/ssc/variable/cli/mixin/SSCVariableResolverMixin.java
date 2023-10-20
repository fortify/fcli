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
package com.fortify.cli.ssc.variable.cli.mixin;

import com.fortify.cli.ssc.variable.helper.SSCVariableDescriptor;
import com.fortify.cli.ssc.variable.helper.SSCVariableHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCVariableResolverMixin {
    private static abstract class AbstractSSCVariableResolverMixin {
        public abstract String getVariableNameOrIdOrGuid();
        
        public SSCVariableDescriptor getVariableDescriptor(UnirestInstance unirest, String appVersionId) {
            return new SSCVariableHelper(unirest, appVersionId).getDescriptorByNameOrIdOrGuid(getVariableNameOrIdOrGuid(), true);
        }
    }
    
    public static class VariableOption extends AbstractSSCVariableResolverMixin {
        @Option(names="--Variable", descriptionKey = "fcli.ssc.variable.resolver.nameOrIdOrGuid")
        @Getter private String VariableNameOrIdOrGuid;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCVariableResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.variable.resolver.nameOrIdOrGuid")
        @Getter private String VariableNameOrIdOrGuid;
    }
}
