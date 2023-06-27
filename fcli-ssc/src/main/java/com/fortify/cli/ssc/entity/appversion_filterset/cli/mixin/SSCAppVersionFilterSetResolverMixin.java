/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.appversion_filterset.cli.mixin;

import com.fortify.cli.ssc.entity.appversion_filterset.helper.SSCAppVersionFilterSetDescriptor;
import com.fortify.cli.ssc.entity.appversion_filterset.helper.SSCAppVersionFilterSetHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionFilterSetResolverMixin {
    private static abstract class AbstractSSCFilterSetResolverMixin {
        public abstract String getFilterSetTitleOrId();
        
        public SSCAppVersionFilterSetDescriptor getFilterSetDescriptor(UnirestInstance unirest, String appVersionId) {
            return new SSCAppVersionFilterSetHelper(unirest, appVersionId).getDescriptorByTitleOrId(getFilterSetTitleOrId(), true);
        }
    }
    
    public static class FilterSetOption extends AbstractSSCFilterSetResolverMixin {
        @Option(names="--filterset")
        @Getter private String filterSetTitleOrId;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCFilterSetResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "filterSetTitleOrId")
        @Getter private String filterSetTitleOrId;
    }
}
