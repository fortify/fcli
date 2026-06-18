/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.license.ncd_report.cli.cmd;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;
import com.fortify.cli.license.ncd_report.writer.NcdReportContributorsCsvSchema;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-contributors")
public final class NcdReportListContributorsCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;

    @Option(names = {"-r", "--report"}, required = true)
    @Getter private Path reportPath;

    public enum IncludeContributor {
        duplicates, ignored
    }

    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--include"}, split = ",")
    @Getter private Set<IncludeContributor> include = new HashSet<>();

    @Override
    public JsonNode getJsonNode() {
        var result = JsonHelper.getObjectMapper().createArrayNode();
        try ( var reader = new NcdReportReader(reportPath) ) {
            var contributors = readContributors(reader);
            contributors.stream()
                .filter(this::shouldInclude)
                .sorted(compareByNameEmailAuthorId())
                .map(this::toOutputRow)
                .forEach(result::add);
        }
        return result;
    }

    private List<Map<String, String>> readContributors(NcdReportReader reader) {
        return reader.readContributors();
    }

    private boolean shouldInclude(Map<String, String> row) {
        var status = row.getOrDefault("contributionStatus", "").toLowerCase();
        if ( "duplicate".equals(status) && !include.contains(IncludeContributor.duplicates) ) {
            return false;
        }
        if ( "ignored".equals(status) && !include.contains(IncludeContributor.ignored) ) {
            return false;
        }
        return true;
    }

    private Comparator<Map<String, String>> compareByNameEmailAuthorId() {
        return Comparator
                .<Map<String, String>, String>comparing(r -> r.getOrDefault("authorName", "").toLowerCase())
                .thenComparing(r -> r.getOrDefault("authorEmail", "").toLowerCase())
                .thenComparing(r -> r.getOrDefault("authorId", ""));
    }

    private ObjectNode toOutputRow(Map<String, String> row) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put(NcdReportContributorsCsvSchema.AUTHOR_ID, getValue(row, NcdReportContributorsCsvSchema.AUTHOR_ID))
                .put(NcdReportContributorsCsvSchema.AUTHOR_NAME, getValue(row, NcdReportContributorsCsvSchema.AUTHOR_NAME))
                .put(NcdReportContributorsCsvSchema.AUTHOR_EMAIL, getValue(row, NcdReportContributorsCsvSchema.AUTHOR_EMAIL))
                .put("cleanName", getValue(row, "cleanName"))
                .put("cleanEmailName", getValue(row, "cleanEmailName"))
                .put(NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS, getValue(row, NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS))
                .put(NcdReportContributorsCsvSchema.CONTRIBUTING_AUTHOR_NUMBER, getValue(row, NcdReportContributorsCsvSchema.CONTRIBUTING_AUTHOR_NUMBER))
                .put(NcdReportContributorsCsvSchema.AI_DUPLICATE_OF, getValue(row, NcdReportContributorsCsvSchema.AI_DUPLICATE_OF))
                .put(NcdReportContributorsCsvSchema.AI_CONFIDENCE, getValue(row, NcdReportContributorsCsvSchema.AI_CONFIDENCE))
                .put(NcdReportContributorsCsvSchema.AI_NOTES, getValue(row, NcdReportContributorsCsvSchema.AI_NOTES))
                .put(NcdReportContributorsCsvSchema.OVERRIDDEN_STATUS, getValue(row, NcdReportContributorsCsvSchema.OVERRIDDEN_STATUS));
    }

    private String getValue(Map<String, String> row, String fieldName) {
        return StringUtils.defaultString(row.get(fieldName));
    }

    @Override
    public final boolean isSingular() {
        return false;
    }
}
