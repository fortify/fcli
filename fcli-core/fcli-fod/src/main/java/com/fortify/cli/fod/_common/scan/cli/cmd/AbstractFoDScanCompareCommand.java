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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.helper.compare.FoDScanCompareHelper;
import com.fortify.cli.fod._common.scan.helper.compare.IFoDScanCompareSupport;
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

    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        requireMinimum("--max-issues", maxIssues, 0);
        var compareSupport = getCompareSupport();
        var release = releaseResolver.getReleaseDescriptor(unirest);
        var releaseId = release.getReleaseId();
        var pair = FoDScanCompareHelper.getScanPair(unirest, releaseId, compareSupport, ignoreCanceled);
        var result = FoDScanCompareHelper.createResult(release, pair);
        FoDScanCompareHelper.addIssueDeltas(unirest, releaseId, compareSupport.getScanType().name(), pair, maxIssues, result);
        FoDScanCompareHelper.addSettingChanges(result, pair, compareSupport.getSettingFields());
        FoDScanCompareHelper.addStatistics(result, pair, compareSupport.getStatisticFields());
        compareSupport.addScanTypeDeltas(unirest, pair, result);
        return result;
    }

    protected abstract IFoDScanCompareSupport getCompareSupport();

    protected static int requireMinimum(String optionName, int value, int minimum) {
        if ( value < minimum ) {
            throw new FcliSimpleException(String.format("Option %s must be at least %d", optionName, minimum));
        }
        return value;
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
}
