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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.fod.report.helper.FoDReportDescriptor;
import com.fortify.cli.fod.report.helper.FoDReportHelper;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FoDReportResolverMixin {

    public static abstract class AbstractFoDReportResolverMixin {
        public abstract String getReportId();

        public FoDReportDescriptor getReportDescriptor(UnirestInstance unirest) {
            return FoDReportHelper.getReportDescriptor(unirest, getReportId());
        }

        public Collection<JsonNode> getReportDescriptorJsonNodes(UnirestInstance unirest) {
            return Stream.of(getReportDescriptor(unirest)).map(FoDReportDescriptor::asJsonNode).collect(Collectors.toList());
        }

    }

    public static class RequiredOption extends AbstractFoDReportResolverMixin {
        @EnvSuffix("report") @Option(names = {"--report-id"}, required = true)
        @Getter private String reportId;
    }

    public static class PositionalParameter extends AbstractFoDReportResolverMixin {
        @EnvSuffix("report") @Parameters(index = "0", arity = "1", paramLabel="report-id", descriptionKey = "fcli.fod.report.report-id")
        @Getter private String reportId;
    }

}
