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
package com.fortify.cli.aviator._common.output.cli.cmd;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.cli.mixin.ApplyRemediationsOptionsMixin;
import com.fortify.cli.aviator._common.remediations_cache.IRemediationsFprSource;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractAviatorApplyRemediationsCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier, IRecordTransformer {

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin protected ApplyRemediationsOptionsMixin applyOptions;

    @Override
    public final JsonNode getJsonNode() {
        validateSourceSelector();
        requireSourceDir();
        requireIssueIdsCacheOnly();
        Set<String> issueIdFilter = AviatorIssueIdFilterUtils.normalizeIssueIds(applyOptions.getIssueIds());
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            try (IRemediationsFprSource source = openFprSource(logger, progressWriter)) {
                ApplyResult result = RemediationsApplyHelper.apply(source, applyOptions, issueIdFilter, logger);
                return buildResultNode(source, result, issueIdFilter);
            }
        }
    }

    /** Override to validate source selector state before options are checked; no-op by default. */
    protected void validateSourceSelector() {}

    protected abstract boolean isCacheMode();

    protected abstract IRemediationsFprSource openFprSource(AviatorLoggerImpl logger, IProgressWriter progressWriter);

    protected abstract JsonNode buildResultNode(IRemediationsFprSource source, ApplyResult result, Set<String> issueIdFilter);

    @Override
    public final boolean isSingular() { return true; }

    @Override
    public JsonNode transformRecord(JsonNode record) { return record; }

    private void requireSourceDir() {
        FcliSimpleException.throwIf(applyOptions.getSourceCodeDirectory() == null || applyOptions.getSourceCodeDirectory().isBlank(),
                "--source-dir must specify a valid directory path");
    }

    private void requireIssueIdsCacheOnly() {
        List<String> issueIds = applyOptions.getIssueIds();
        FcliSimpleException.throwIf(
                issueIds != null && !issueIds.isEmpty() && !isCacheMode(),
                "--issue-ids can only be used with --from-cache; "
                        + "create a cache with download-remediations-cache and rerun with --from-cache");
    }
}
