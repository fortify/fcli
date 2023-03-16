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
package com.fortify.cli.ssc.report_template.cli.mixin;

import com.fortify.cli.ssc.report_template.helper.SSCReportTemplateDescriptor;
import com.fortify.cli.ssc.report_template.helper.SSCReportTemplateHelper;

import kong.unirest.UnirestInstance;
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
