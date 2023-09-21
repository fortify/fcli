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
package com.fortify.cli.ssc.appversion_performanceindicator.cli.mixin;

import com.fortify.cli.ssc.appversion_performanceindicator.helper.SSCAppVersionPerformanceIndicatorDescriptor;
import com.fortify.cli.ssc.appversion_performanceindicator.helper.SSCAppVersionPerformanceIndicatorHelper;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionPerformanceIndicatorResolverMixin {
    private static abstract class AbstractSSCPerformanceIndicatorResolverMixin {
        public abstract String getPerformanceIndicatorId();
        
        public SSCAppVersionPerformanceIndicatorDescriptor getPerformanceIndicatorDescriptor(UnirestInstance unirest, String appVersionId) {
            return new SSCAppVersionPerformanceIndicatorHelper(unirest, appVersionId).getDescriptorById(getPerformanceIndicatorId(), true);
        }
    }
    
    public static class PerformanceIndicatorOption extends AbstractSSCPerformanceIndicatorResolverMixin {
        @Option(names="--performanceindicator", descriptionKey = "fcli.ssc.appversion-performance-indicator.resolver.id")
        @Getter private String performanceIndicatorId;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCPerformanceIndicatorResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.appversion-performance-indicator.resolver.id")
        @Getter private String performanceIndicatorId;
    }
}
