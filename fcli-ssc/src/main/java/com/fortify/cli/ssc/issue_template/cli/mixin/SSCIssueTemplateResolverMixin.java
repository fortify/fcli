/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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
    
    public static class OptionalFilterSetOption extends AbstractSSCIssueTemplateResolverMixin {
        @Option(names="--issue-template", descriptionKey = "issueTemplateNameOrId", required = false)
        @Getter private String issueTemplateNameOrId;
    }
    
    public static class PositionalParameterSingle extends AbstractSSCIssueTemplateResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "issueTemplateNameOrId")
        @Getter private String issueTemplateNameOrId;
    }
}
