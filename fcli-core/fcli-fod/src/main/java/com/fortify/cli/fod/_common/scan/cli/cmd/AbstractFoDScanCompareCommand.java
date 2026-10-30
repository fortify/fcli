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
package com.fortify.cli.fod._common.scan.cli.cmd;

import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.helper.compare.FoDScanCompareHelper;
import com.fortify.cli.fod._common.scan.helper.compare.IFoDScanCompareSupport;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionResult;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@CommandGroup("*-scan-compare")
public abstract class AbstractFoDScanCompareCommand extends AbstractFoDJsonNodeOutputCommand {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Option(names = {"--ignore-canceled"}, descriptionKey = "fcli.fod.scan.compare.ignore-canceled") private boolean ignoreCanceled;
    @Option(names = {"--max-issues"}, defaultValue = "1000", paramLabel = "<count>",
            descriptionKey = "fcli.fod.scan.compare.max-issues") private int maxIssues;
    @Option(names = {"--no-attribution"}, negatable = true,
            descriptionKey = "fcli.fod.scan.compare.no-attribution") private boolean attribution = true;
    @Option(names = {"--limit-change-details"},
            descriptionKey = "fcli.fod.scan.compare.limit-change-details") private boolean limitChangeDetails;
    @Option(names = {"--change-threshold"}, defaultValue = "10", paramLabel = "<percent>",
            descriptionKey = "fcli.fod.scan.compare.change-threshold") private int changeThresholdPercent;

    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        requireMinimum("--max-issues", maxIssues, 0);
        requireRange("--change-threshold", changeThresholdPercent, 1, 100);
        var compareSupport = getCompareSupport();
        var release = releaseResolver.getReleaseDescriptor(unirest);
        var releaseId = release.getReleaseId();
        var pair = FoDScanCompareHelper.getScanPair(unirest, releaseId, compareSupport, ignoreCanceled);
        var issues = FoDScanCompareHelper.loadIssues(unirest, releaseId, compareSupport, pair);
        var changes = compareSupport.loadChanges(unirest, pair);
        var attributionResult = !attribution ? null : FoDScanCompareHelper.attributeIssues(
                unirest, releaseId, pair, compareSupport, issues, changes, changeThresholdPercent);
        var result = FoDScanCompareHelper.createResult(release, pair);
        issues.addTo(result, maxIssues);
        FoDScanCompareHelper.addSettingChanges(result, pair, compareSupport.getSettingFields());
        FoDScanCompareHelper.addStatistics(result, pair, compareSupport.getStatisticFields());
        result.put("changeDetailsLimited", limitChangeDetails);
        changes.addTo(result, getChangeFilter(attributionResult));
        if ( attributionResult != null ) {
            attributionResult.addTo(result);
        } else {
            result.put("attributionEnabled", false);
        }
        return result;
    }

    protected abstract IFoDScanCompareSupport getCompareSupport();

    // With --limit-change-details, only changes referenced by issue attributions are listed
    private Predicate<String> getChangeFilter(FoDAttributionResult attributionResult) {
        if ( !limitChangeDetails ) { return change -> true; }
        if ( attributionResult == null ) { return change -> false; }
        var referenced = attributionResult.getReferencedChanges();
        return referenced::contains;
    }

    protected static int requireMinimum(String optionName, int value, int minimum) {
        if ( value < minimum ) {
            throw new FcliSimpleException(String.format("Option %s must be at least %d", optionName, minimum));
        }
        return value;
    }

    private static int requireRange(String optionName, int value, int minimum, int maximum) {
        if ( value < minimum || value > maximum ) {
            throw new FcliSimpleException(String.format("Option %s must be between %d and %d", optionName, minimum, maximum));
        }
        return value;
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
}
