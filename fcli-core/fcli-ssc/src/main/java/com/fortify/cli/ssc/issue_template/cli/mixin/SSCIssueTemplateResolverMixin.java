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
package com.fortify.cli.ssc.issue_template.cli.mixin;

import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCIssueTemplateResolverMixin {
    private static abstract class AbstractSSCIssueTemplateResolverMixin {
        public abstract String getIssueTemplateNameOrId();
        
        public SSCIssueTemplateDescriptor getIssueTemplateDescriptor(UnirestInstance unirest) {
            String issueTemplateNameOrId = getIssueTemplateNameOrId();
            return StringUtils.isBlank(issueTemplateNameOrId) 
                    ? null 
                    : new SSCIssueTemplateHelper(unirest).getDescriptorByNameOrId(issueTemplateNameOrId, true);
        }
        
        public SSCIssueTemplateDescriptor getIssueTemplateDescriptorOrDefault(UnirestInstance unirest) {
            SSCIssueTemplateDescriptor descriptor = getIssueTemplateDescriptor(unirest);
            return descriptor!=null ? descriptor : SSCIssueTemplateHelper.getDefaultIssueTemplateDescriptor(unirest);
        }
    }
    
    public static class OptionalOption extends AbstractSSCIssueTemplateResolverMixin {
        @Option(names="--issue-template", required = false, descriptionKey = "fcli.ssc.issue-template.resolver.nameOrId")
        @Getter private String issueTemplateNameOrId;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCIssueTemplateResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.issue-template.resolver.nameOrId")
        @Getter private String issueTemplateNameOrId;
    }
}
