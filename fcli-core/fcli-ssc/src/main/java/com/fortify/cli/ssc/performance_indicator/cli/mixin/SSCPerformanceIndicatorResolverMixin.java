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
package com.fortify.cli.ssc.performance_indicator.cli.mixin;

import com.fortify.cli.ssc.performance_indicator.helper.SSCPerformanceIndicatorDescriptor;
import com.fortify.cli.ssc.performance_indicator.helper.SSCPerformanceIndicatorHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCPerformanceIndicatorResolverMixin {
    private static abstract class AbstractSSCPerformanceIndicatorResolverMixin {
        public abstract String getPerformanceIndicatorNameOrIdOrGuid();
        
        public SSCPerformanceIndicatorDescriptor getPerformanceIndicatorDescriptor(UnirestInstance unirest, String appVersionId) {
            return new SSCPerformanceIndicatorHelper(unirest, appVersionId).getDescriptorByNameOrIdOrGuid(getPerformanceIndicatorNameOrIdOrGuid(), true);
        }
    }
    
    public static class PerformanceIndicatorOption extends AbstractSSCPerformanceIndicatorResolverMixin {
        @Option(names="--performanceindicator", descriptionKey = "fcli.ssc.performance-indicator.resolver.nameOrIdOrGuid")
        @Getter private String performanceIndicatorNameOrIdOrGuid;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCPerformanceIndicatorResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.performance-indicator.resolver.nameOrIdOrGuid")
        @Getter private String performanceIndicatorNameOrIdOrGuid;
    }
}
