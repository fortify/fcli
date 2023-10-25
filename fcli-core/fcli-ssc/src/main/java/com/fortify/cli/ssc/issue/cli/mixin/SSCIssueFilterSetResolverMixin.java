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
package com.fortify.cli.ssc.issue.cli.mixin;

import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetDescriptor;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCIssueFilterSetResolverMixin {
    private static abstract class AbstractSSCFilterSetResolverMixin {
        public abstract String getFilterSetTitleOrId();
        
        public SSCIssueFilterSetDescriptor getFilterSetDescriptor(UnirestInstance unirest, String appVersionId) {
            return new SSCIssueFilterSetHelper(unirest, appVersionId).getDescriptorByTitleOrId(getFilterSetTitleOrId(), true);
        }
    }
    
    public static class FilterSetOption extends AbstractSSCFilterSetResolverMixin {
        @Option(names="--filterset", descriptionKey = "fcli.ssc.issue.filterset.resolver.titleOrId")
        @Getter private String filterSetTitleOrId;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCFilterSetResolverMixin {
        @EnvSuffix("FILTERSET") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.issue.filterset.resolver.titleOrId")
        @Getter private String filterSetTitleOrId;
    }
}
