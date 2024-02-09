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
package com.fortify.cli.fod.report.cli.mixin;

import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod.report.helper.FoDReportTemplateDescriptor;
import com.fortify.cli.fod.report.helper.FoDReportTemplateHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDReportTemplateByNameOrIdResolverMixin {
    public static abstract class AbstractFoDReportTemplateNameOrIdResolverMixin {
        public abstract String getReportTemplateNameOrId();

        public FoDReportTemplateDescriptor getReportTemplateDescriptor(UnirestInstance unirest) {
            var reportTemplateNameOrId = getReportTemplateNameOrId();
            return StringUtils.isBlank(reportTemplateNameOrId)
                    ? null
                    : FoDReportTemplateHelper.getReportTemplateDescriptor(unirest, reportTemplateNameOrId, true);
        }

        public String getReportTemplateId(UnirestInstance unirest) {
            var descriptor = getReportTemplateDescriptor(unirest);
            return descriptor==null ? null : descriptor.getValue();
        }
    }

    public static class RequiredOption extends AbstractFoDReportTemplateNameOrIdResolverMixin {
        @Option(names = {"--template"}, required = true, paramLabel = "id|name", descriptionKey = "fcli.fod.report.report-template.name-or-id")
        @Getter private String reportTemplateNameOrId;
    }

    public static class OptionalOption extends AbstractFoDReportTemplateNameOrIdResolverMixin {
        @Option(names = {"--template"}, required = false, paramLabel = "id|name", descriptionKey = "fcli.fod.report.report-template.name-or-id")
        @Getter private String reportTemplateNameOrId;
    }

}
