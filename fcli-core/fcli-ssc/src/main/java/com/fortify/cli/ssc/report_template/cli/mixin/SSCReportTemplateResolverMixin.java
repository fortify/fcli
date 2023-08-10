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
package com.fortify.cli.ssc.report_template.cli.mixin;

import com.fortify.cli.ssc.report_template.helper.SSCReportTemplateDescriptor;
import com.fortify.cli.ssc.report_template.helper.SSCReportTemplateHelper;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCReportTemplateResolverMixin {
    private static abstract class AbstractSSCReportTemplateResolverMixin {
        protected abstract String getReportTemplateNameOrId();
        public SSCReportTemplateDescriptor getReportTemplateDescriptor(UnirestInstance unirest) {
            return new SSCReportTemplateHelper(unirest).getDescriptorByNameOrId(getReportTemplateNameOrId(), true);
        }
    }
    public static class PositionalParameterSingle extends AbstractSSCReportTemplateResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "reportTemplateNameOrId")
        @Getter private String reportTemplateNameOrId;
    }
    public static class RequiredOption extends AbstractSSCReportTemplateResolverMixin {
        @Option(names="--report-template", required=true, descriptionKey = "reportTemplateNameOrId")
        @Getter private String reportTemplateNameOrId;
    }
    
}
