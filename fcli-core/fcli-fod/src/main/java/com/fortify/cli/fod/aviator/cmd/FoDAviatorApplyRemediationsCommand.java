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
package com.fortify.cli.fod.aviator.cmd;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.remediations_cache.CacheRemediationsFprSource;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator._common.util.AviatorApplyRemediationsCliSupport;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.aviator.cli.mixin.FoDAviatorApplyRemediationsSourceMixin;
import com.fortify.cli.fod.aviator.helper.AviatorFoDApplyRemediationsHelper;
import com.fortify.cli.fod.aviator.helper.FoDOnlineRemediationsFprSource;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class FoDAviatorApplyRemediationsCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(FoDAviatorApplyRemediationsCommand.class);

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Injected into sourceSelector
    @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Mixin private FoDAviatorApplyRemediationsSourceMixin sourceSelector;

    @Option(names = {"--source-dir"})
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",")
    private List<String> issueIds;
    @Option(names = {"--preview"})
    private boolean previewMode = false;

    @Override
    public JsonNode getJsonNode() {
        AviatorApplyRemediationsCliSupport.requireSourceDir(sourceCodeDirectory);
        Set<String> issueIdFilter = AviatorApplyRemediationsCliSupport.normalizeIssueIdsForCacheOnly(
                issueIds, sourceSelector.isFromCacheSelected());

        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (sourceSelector.isFromCacheSelected()) {
                return processFromCache(logger, issueIdFilter);
            }
            return processOnline(logger, issueIdFilter);
        }
    }

    private JsonNode processOnline(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
        FoDReleaseDescriptor release = sourceSelector.getReleaseDescriptor(unirest);
        try (FoDOnlineRemediationsFprSource source =
                new FoDOnlineRemediationsFprSource(unirest, logger, release)) {
            ApplyResult applyResult = RemediationsApplyHelper.apply(
                    source, sourceCodeDirectory, logger, issueIdFilter, LOG, previewMode);
            return AviatorFoDApplyRemediationsHelper.buildOnlineResultNode(release, applyResult);
        }
    }

    private JsonNode processFromCache(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        try (CacheRemediationsFprSource source = CacheRemediationsFprSource.open(
                sourceSelector.getFromCache(),
                RemediationsCacheConstants.PRODUCT_FOD)) {
            ApplyResult applyResult = RemediationsApplyHelper.apply(
                    source, sourceCodeDirectory, logger, issueIdFilter, LOG, previewMode);
            return AviatorFoDApplyRemediationsHelper.buildCacheResultNode(
                    sourceSelector.getFromCache(), applyResult, issueIdFilter);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return previewMode ? "Remediation-Previewed" : "Remediation-Applied";
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return record;
    }
}
