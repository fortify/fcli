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
import com.fortify.cli.aviator._common.remediations_cache.CacheRemediationsFprSource;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCApplyRemediationsSourceMixin;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsSelectorArgGroups.OnlineSelectionArgGroup.ResolvedOnlineArtifacts;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCApplyRemediationsHelper;
import com.fortify.cli.aviator.ssc.helper.SinceOptionHelper;
import com.fortify.cli.aviator.ssc.helper.SscOnlineRemediationsFprSource;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.cli.mixin.SSCUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
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

    @Option(names = {"--source-dir"})
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",")
    private List<String> issueIds;

    @Override
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
        try (CacheRemediationsFprSource source = CacheRemediationsFprSource.open(
                sourceSelector.getFromCache(),
                RemediationsCacheConstants.PRODUCT_SSC,
                CacheRemediationsFprSource.IdKind.ARTIFACT_ID)) {
            ApplyResult applyResult = RemediationsApplyHelper.apply(
                    source, sourceCodeDirectory, logger, issueIdFilter, LOG);
            return AviatorSSCApplyRemediationsHelper.buildCacheResultNode(
                    sourceSelector.getFromCache(),
                    applyResult,
                    issueIdFilter,
                    source.reader().getManifest().getSelection());
        }
    }

    private JsonNode processOnline(
            AviatorLoggerImpl logger, IProgressWriter progressWriter, Set<String> issueIdFilter) {
        UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
        OffsetDateTime sinceDate = SinceOptionHelper.parse(sourceSelector.getSince());
        // One resolve: artifacts + appVersionId (no second getAppVersionId REST call).
        ResolvedOnlineArtifacts resolved = sourceSelector.resolveArtifacts(unirest, sinceDate);
        try (SscOnlineRemediationsFprSource source = new SscOnlineRemediationsFprSource(
                unirest, logger, progressWriter, resolved.artifacts())) {
            ApplyResult applyResult = RemediationsApplyHelper.apply(
                    source, sourceCodeDirectory, logger, issueIdFilter, LOG);
            return AviatorSSCApplyRemediationsHelper.buildOnlineResultNode(
                    resolved.artifacts(), resolved.appVersionId(), applyResult, issueIdFilter);
        }
    }

    private void validateSourceCodeDirectory() {
        FcliSimpleException.throwIf(sourceCodeDirectory == null || sourceCodeDirectory.isBlank(),
                "--source-dir must specify a valid directory path");
    }

    private void validateIssueIdFilterMode() {
        FcliSimpleException.throwIf(
                issueIds != null && !issueIds.isEmpty() && !sourceSelector.isFromCacheSelected(),
                "--issue-ids can only be used with --from-cache; "
                        + "create a cache with download-remediations-cache and rerun with --from-cache");
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
