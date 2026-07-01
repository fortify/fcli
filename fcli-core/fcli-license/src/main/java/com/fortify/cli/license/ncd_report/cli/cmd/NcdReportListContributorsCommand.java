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
            // Sort contributors by author name with duplicates immediately following their representative,
            // and ignored records at the end. This sorting is applied for consistency and readability,
            // especially when reading legacy reports that may not be optimally ordered.
            var deduped = dedupeByAuthorId(contributors);
            var asObjectNodes = deduped.stream()
                .map(map -> JsonHelper.getObjectMapper().convertValue(map, ObjectNode.class))
                .toList();
            var sorted = NcdReportContributorsCsvSchema.sortByAuthorNameAndStatus(new java.util.ArrayList<>(asObjectNodes));
            sorted.stream()
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
            .filter(r -> StringUtils.isBlank(getValue(r, NcdReportContributorsCsvSchema.DUPLICATE_OF)))
            .forEach(r -> {
                var number = getValue(r, "contributingAuthorNumber");
                var representativeId = representativeByContributingNumber.get(number);
                if ( StringUtils.isNotBlank(representativeId)
                        && !representativeId.equals(getValue(r, NcdReportContributorsCsvSchema.AUTHOR_ID)) ) {
                    r.put(NcdReportContributorsCsvSchema.DUPLICATE_OF, representativeId);
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

    private ObjectNode toOutputRow(ObjectNode row) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put(NcdReportContributorsCsvSchema.AUTHOR_ID, row.path(NcdReportContributorsCsvSchema.AUTHOR_ID).asText(""))
                .put(NcdReportContributorsCsvSchema.AUTHOR_NAME, row.path(NcdReportContributorsCsvSchema.AUTHOR_NAME).asText(""))
                .put(NcdReportContributorsCsvSchema.AUTHOR_EMAIL, row.path(NcdReportContributorsCsvSchema.AUTHOR_EMAIL).asText(""))
                .put(NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS, row.path(NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS).asText(""))
                .put(NcdReportContributorsCsvSchema.DUPLICATE_OF, row.path(NcdReportContributorsCsvSchema.DUPLICATE_OF).asText(""))
                .put(NcdReportContributorsCsvSchema.OVERRIDE_STATUS, row.path(NcdReportContributorsCsvSchema.OVERRIDE_STATUS).asText(""))
                .put(NcdReportContributorsCsvSchema.OVERRIDE_STATUS_CONFIDENCE,
                        row.path(NcdReportContributorsCsvSchema.OVERRIDE_STATUS_CONFIDENCE).asText(""))
                .put(NcdReportContributorsCsvSchema.OVERRIDE_STATUS_NOTES,
                        row.path(NcdReportContributorsCsvSchema.OVERRIDE_STATUS_NOTES).asText(""));
    }

    private String getValue(Map<String, String> row, String fieldName) {
        return StringUtils.defaultString(row.get(fieldName));
    }

    @Override
    public final boolean isSingular() {
        return false;
    }
}
