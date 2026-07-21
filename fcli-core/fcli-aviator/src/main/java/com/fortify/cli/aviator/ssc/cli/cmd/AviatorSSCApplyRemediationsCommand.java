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
package com.fortify.cli.aviator.ssc.cli.cmd;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheReader;
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCApplyRemediationsSourceMixin;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCApplyRemediationsHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCOnlineRemediationsApplier;
import com.fortify.cli.aviator.ssc.helper.SinceOptionHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.cli.mixin.SSCUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class AviatorSSCApplyRemediationsCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCApplyRemediationsCommand.class);

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private AviatorSSCApplyRemediationsSourceMixin sourceSelector;
    @Mixin private SSCUnirestInstanceSupplierMixin unirestInstanceSupplier;

    @Option(names = {"--source-dir"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.source-dir")
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",", descriptionKey = "fcli.aviator.ssc.apply-remediations.issue-ids")
    private List<String> issueIds;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode() {
        sourceSelector.validate();
        validateSourceCodeDirectory();
        Set<String> issueIdFilter = AviatorIssueIdFilterUtils.normalizeIssueIds(issueIds);
        validateIssueIdFilterMode();

        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (sourceSelector.isFromCacheSelected()) {
                return processFromCache(logger, issueIdFilter);
            }
            return processOnline(logger, progressWriter, issueIdFilter);
        }
    }

    private JsonNode processFromCache(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        try (RemediationsCacheReader cacheReader = RemediationsCacheReader.open(sourceSelector.getFromCache())) {
            ApplyResult applyResult = RemediationsCacheApplyHelper.applyEntries(
                    cacheReader,
                    RemediationsCacheConstants.PRODUCT_SSC,
                    sourceCodeDirectory,
                    logger,
                    issueIdFilter,
                    RemediationsCacheApplyHelper.EntryIdKind.ARTIFACT_ID,
                    LOG);
            RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                    issueIdFilter, applyResult.metrics());
            String appVersionId = cacheReader.getManifest().getSelection() != null
                    ? cacheReader.getManifest().getSelection().get("appVersionId")
                    : null;
            return AviatorSSCApplyRemediationsHelper.buildCacheResultNode(
                    new AviatorSSCApplyRemediationsHelper.CacheResultData(
                            sourceSelector.getFromCache(),
                            applyResult.processedEntries(),
                            applyResult.processedIds(),
                            appVersionId,
                            applyResult.metrics().size(),
                            applyResult.skipped(),
                            aggregated.totalRemediations(),
                            aggregated.appliedRemediations(),
                            aggregated.skippedRemediations(),
                            aggregated.modifiedFiles(),
                            RemediationsCacheApplyHelper.actionLabel(aggregated)));
        }
    }

    private JsonNode processOnline(
            AviatorLoggerImpl logger, IProgressWriter progressWriter, Set<String> issueIdFilter) {
        UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
        AviatorSSCOnlineRemediationsApplier applier = new AviatorSSCOnlineRemediationsApplier(
                unirest, logger, progressWriter, sourceCodeDirectory, issueIdFilter);
        OffsetDateTime sinceDate = SinceOptionHelper.parse(sourceSelector.getSince());
        if (sourceSelector.isAllSelected()) {
            return applier.applyAll(sourceSelector.getAppVersionId(unirest), sinceDate);
        }
        return applier.applyOne(resolveArtifactDescriptor(unirest, sinceDate));
    }

    private SSCArtifactDescriptor resolveArtifactDescriptor(UnirestInstance unirest, OffsetDateTime sinceDate) {
        if (sourceSelector.isLatestSelected()) {
            return SSCArtifactHelper.getLatestAviatorArtifact(
                    unirest, sourceSelector.getAppVersionId(unirest), sinceDate);
        }
        return SSCArtifactHelper.getArtifactDescriptor(unirest, sourceSelector.getArtifactId());
    }

    private void validateSourceCodeDirectory() {
        if (sourceCodeDirectory == null || sourceCodeDirectory.isBlank()) {
            throw new FcliSimpleException("--source-dir must specify a valid directory path");
        }
    }

    private void validateIssueIdFilterMode() {
        if (issueIds != null && !issueIds.isEmpty() && !sourceSelector.isFromCacheSelected()) {
            throw new FcliSimpleException(
                    "--issue-ids can only be used with --from-cache; create a cache with download-remediations-cache and rerun with --from-cache");
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return "Remediations Applied";
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return record;
    }
}
