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
import com.fortify.cli.ssc.issue.helper.SSCIssueGroupDescriptor;
import com.fortify.cli.ssc.issue.helper.SSCIssueGroupHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCIssueGroupResolverMixin {
    private static abstract class AbstractSSCGroupSetResolverMixin {
        public abstract String getGroupSetDisplayNameOrId();
        
        public SSCIssueGroupDescriptor getGroupSetDescriptor(UnirestInstance unirest, String appVersionId) {
            return new SSCIssueGroupHelper(unirest, appVersionId).getDescriptorByDisplayNameOrId(getGroupSetDisplayNameOrId(), true);
        }
    }
    
    public static class GroupByOption extends AbstractSSCGroupSetResolverMixin {
        @Option(names="--by", defaultValue = "FOLDER", descriptionKey = "fcli.ssc.issue.group.resolver.displayNameOrId")
        @Getter public String groupSetDisplayNameOrId;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCGroupSetResolverMixin {
        @EnvSuffix("BY") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.issue.group.resolver.displayNameOrId")
        @Getter private String groupSetDisplayNameOrId;
    }
}
