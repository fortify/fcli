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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.license.ncd_report.helper.NcdReportContributorHelper;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;
import com.fortify.cli.license.ncd_report.writer.NcdReportContributorsCsvSchema;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-contributors", aliases = {"lsc"})
public final class NcdReportListContributorsCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;

    @Option(names = {"-r", "--report"}, required = true)
    @Getter private Path reportPath;

    @Override
    public JsonNode getJsonNode() {
        var result = JsonHelper.getObjectMapper().createArrayNode();
        try ( var reader = new NcdReportReader(reportPath) ) {
            var contributors = readContributors(reader);
            enrichContributors(contributors);
            dedupeByAuthorId(contributors).stream()
                .sorted(compareByNameEmailAuthorId())
                .map(this::toOutputRow)
                .forEach(result::add);
        }
        return result;
    }

    private List<Map<String, String>> readContributors(NcdReportReader reader) {
        return reader.readContributors();
    }

    private void enrichContributors(List<Map<String, String>> contributors) {
        var representativeByContributingNumber = new java.util.HashMap<String, String>();

        // Ensure authorId is present (legacy reports may not include this column).
        contributors.forEach(this::ensureAuthorId);

        contributors.stream()
            .filter(r -> "contributing".equalsIgnoreCase(getValue(r, NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS)))
            .forEach(r -> {
                var number = getValue(r, "contributingAuthorNumber");
                if ( StringUtils.isNotBlank(number) ) {
                    representativeByContributingNumber.put(number, getValue(r, NcdReportContributorsCsvSchema.AUTHOR_ID));
                }
            });

        contributors.stream()
            .filter(r -> "duplicate".equalsIgnoreCase(getValue(r, NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS)))
            .filter(r -> StringUtils.isBlank(getValue(r, NcdReportContributorsCsvSchema.OVERRIDE_DUPLICATE_OF)))
            .forEach(r -> {
                var number = getValue(r, "contributingAuthorNumber");
                var representativeId = representativeByContributingNumber.get(number);
                if ( StringUtils.isNotBlank(representativeId)
                        && !representativeId.equals(getValue(r, NcdReportContributorsCsvSchema.AUTHOR_ID)) ) {
                    r.put(NcdReportContributorsCsvSchema.OVERRIDE_DUPLICATE_OF, representativeId);
                }
            });
    }

    private void ensureAuthorId(Map<String, String> row) {
        if ( StringUtils.isNotBlank(getValue(row, NcdReportContributorsCsvSchema.AUTHOR_ID)) ) {
            return;
        }
        var expressionInput = NcdReportContributorHelper.createExpressionInput(
                getValue(row, NcdReportContributorsCsvSchema.AUTHOR_NAME),
                getValue(row, NcdReportContributorsCsvSchema.AUTHOR_EMAIL));
        row.put(NcdReportContributorsCsvSchema.AUTHOR_ID, NcdReportContributorHelper.computeAuthorId(expressionInput));
    }

    private List<Map<String, String>> dedupeByAuthorId(List<Map<String, String>> contributors) {
        var result = new ArrayList<Map<String, String>>();
        var seenAuthorIds = new HashSet<String>();
        for ( var row : contributors.stream().sorted(compareByAuthorIdPriority()).toList() ) {
            var authorId = getValue(row, NcdReportContributorsCsvSchema.AUTHOR_ID);
            if ( seenAuthorIds.add(authorId) ) {
                result.add(row);
            }
        }
        return result;
    }

    private Comparator<Map<String, String>> compareByAuthorIdPriority() {
        return Comparator
                .<Map<String, String>, String>comparing(r -> r.getOrDefault(NcdReportContributorsCsvSchema.AUTHOR_ID, ""))
                .thenComparingInt(this::contributionStatusPriority)
                .thenComparing(r -> r.getOrDefault(NcdReportContributorsCsvSchema.AUTHOR_NAME, "").toLowerCase())
                .thenComparing(r -> r.getOrDefault(NcdReportContributorsCsvSchema.AUTHOR_EMAIL, "").toLowerCase());
    }

    private int contributionStatusPriority(Map<String, String> row) {
        var status = getValue(row, NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS);
        return switch ( status.toLowerCase() ) {
        case "contributing" -> 0;
        case "duplicate" -> 1;
        case "ignored" -> 2;
        default -> 3;
        };
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
                .put(NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS,
                        getValue(row, NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS))
                .put("duplicateOf",
                        getValue(row, NcdReportContributorsCsvSchema.OVERRIDE_DUPLICATE_OF))
                .put(NcdReportContributorsCsvSchema.OVERRIDE_STATUS,
                        getValue(row, NcdReportContributorsCsvSchema.OVERRIDE_STATUS))
                .put(NcdReportContributorsCsvSchema.OVERRIDE_DUPLICATE_OF,
                        getValue(row, NcdReportContributorsCsvSchema.OVERRIDE_DUPLICATE_OF))
                .put(NcdReportContributorsCsvSchema.OVERRIDE_STATUS_CONFIDENCE,
                        getValue(row, NcdReportContributorsCsvSchema.OVERRIDE_STATUS_CONFIDENCE))
                .put(NcdReportContributorsCsvSchema.OVERRIDE_STATUS_NOTES,
                        getValue(row, NcdReportContributorsCsvSchema.OVERRIDE_STATUS_NOTES));
    }

    private String getValue(Map<String, String> row, String fieldName) {
        return StringUtils.defaultString(row.get(fieldName));
    }

    @Override
    public final boolean isSingular() {
        return false;
    }
}
