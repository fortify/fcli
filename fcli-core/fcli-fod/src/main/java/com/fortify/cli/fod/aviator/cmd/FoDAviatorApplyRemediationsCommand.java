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

import java.nio.file.Path;
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
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.cli.mixin.IFoDDelimiterMixinAware;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.aviator.helper.AviatorFoDApplyRemediationsHelper;
import com.fortify.cli.fod.aviator.helper.AviatorFoDOnlineRemediationsApplier;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
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
    @Mixin private SourceMixin sourceSelector;

    @Option(names = {"--source-dir"}, descriptionKey = "fcli.fod.aviator.apply-remediations.source-dir")
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",", descriptionKey = "fcli.fod.aviator.apply-remediations.issue-ids")
    private List<String> issueIds;

    /**
     * Exclusive source selection: online FoD release (via standard release resolver) or local cache zip.
     * Propagates {@link FoDDelimiterMixin} into the nested release resolver, matching FoD patterns such as
     * {@code FoDAppOrReleaseMixin}.
     */
    public static final class SourceMixin implements IFoDDelimiterMixinAware {
        @ArgGroup(exclusive = true, multiplicity = "1")
        private SourceArgGroup source = new SourceArgGroup();

        @Override
        public void setDelimiterMixin(FoDDelimiterMixin delimiterMixin) {
            if (source != null && source.online != null) {
                source.online.setDelimiterMixin(delimiterMixin);
            }
        }

        public boolean isFromCacheSelected() {
            return source != null && source.fromCache != null;
        }

        public Path getFromCache() {
            return isFromCacheSelected() ? source.fromCache : null;
        }

        public FoDReleaseDescriptor getReleaseDescriptor(UnirestInstance unirest) {
            return source.online.getReleaseDescriptor(unirest);
        }
    }

    @Getter
    static class SourceArgGroup {
        @ArgGroup(exclusive = false, multiplicity = "1")
        private OnlineReleaseArgGroup online = new OnlineReleaseArgGroup();

        @Option(names = {"--from-cache"}, required = true, paramLabel = "<zip>",
                descriptionKey = "fcli.fod.aviator.apply-remediations.from-cache")
        private Path fromCache;
    }

    /** Online release branch reusing the standard FoD release option wiring/resolution. */
    static class OnlineReleaseArgGroup
            extends FoDReleaseByQualifiedNameOrIdResolverMixin.AbstractFoDQualifiedReleaseNameOrIdResolverMixin {
        @Option(names = {"--release", "--rel"}, required = true, paramLabel = "id|app[:ms]:rel",
                descriptionKey = "fcli.fod.release.resolver.name-or-id")
        @Getter private String qualifiedReleaseNameOrId;
    }

    @Override
    @SneakyThrows
    public JsonNode getJsonNode() {
        validateSourceCodeDirectory();
        Set<String> issueIdFilter = AviatorIssueIdFilterUtils.normalizeIssueIds(issueIds);
        validateSelection();

        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (sourceSelector.isFromCacheSelected()) {
                return processFromCache(logger, issueIdFilter);
            }
            UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
            return AviatorFoDOnlineRemediationsApplier.apply(
                    unirest,
                    sourceSelector.getReleaseDescriptor(unirest),
                    sourceCodeDirectory,
                    logger,
                    issueIdFilter);
        }
    }

    private JsonNode processFromCache(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        Path cacheZip = sourceSelector.getFromCache();
        try (RemediationsCacheReader cacheReader = RemediationsCacheReader.open(cacheZip)) {
            ApplyResult applyResult = RemediationsCacheApplyHelper.applyEntries(
                    cacheReader,
                    RemediationsCacheConstants.PRODUCT_FOD,
                    sourceCodeDirectory,
                    logger,
                    issueIdFilter,
                    RemediationsCacheApplyHelper.EntryIdKind.RELEASE_ID,
                    LOG);
            RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                    issueIdFilter, applyResult.metrics());
            return AviatorFoDApplyRemediationsHelper.buildCacheResultNode(
                    new AviatorFoDApplyRemediationsHelper.CacheResultData(
                            cacheZip,
                            applyResult.processedEntries(),
                            applyResult.processedIds(),
                            aggregated.totalRemediations(),
                            aggregated.appliedRemediations(),
                            aggregated.skippedRemediations(),
                            aggregated.modifiedFiles(),
                            RemediationsCacheApplyHelper.actionLabel(aggregated)));
        }
    }

    private void validateSourceCodeDirectory() {
        if (sourceCodeDirectory == null || sourceCodeDirectory.isBlank()) {
            throw new FcliSimpleException("--source-dir must specify a valid directory path");
        }
    }

    private void validateSelection() {
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
        return "Remediation-Applied";
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return record;
    }
}
