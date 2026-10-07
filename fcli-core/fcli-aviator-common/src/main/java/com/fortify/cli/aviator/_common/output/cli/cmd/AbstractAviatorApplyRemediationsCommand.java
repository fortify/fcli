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

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.cli.mixin.AbstractApplyRemediationsOptionsMixin;
import com.fortify.cli.aviator._common.remediations_cache.IRemediationsFprSource;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;

import lombok.Getter;
import picocli.CommandLine.Mixin;

/**
 * Abstract base command for applying remediations. Orchestrates validation, FPR source acquisition,
 * and remediation application. Product-specific subclasses provide options mixin and implement hooks.
 */
public abstract class AbstractAviatorApplyRemediationsCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier {

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;

    /** Subclasses declare their product-specific options mixin (SSC or FoD). */
    protected abstract AbstractApplyRemediationsOptionsMixin getApplyOptions();

    @Override
    public final JsonNode getJsonNode() {
        AbstractApplyRemediationsOptionsMixin applyOptions = getApplyOptions();
        applyOptions.validate();
        Set<String> issueIdFilter = AviatorIssueIdFilterUtils.normalizeIssueIds(applyOptions.getIssueIds());
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            try (IRemediationsFprSource fprSource = openFprSource(logger, progressWriter)) {
                ApplyResult result = RemediationsApplyHelper.apply(fprSource, applyOptions, issueIdFilter, logger);
                return buildResultNode(fprSource, result, issueIdFilter);
            }
        }
    }

    protected abstract IRemediationsFprSource openFprSource(AviatorLoggerImpl logger, IProgressWriter progressWriter);

    protected abstract JsonNode buildResultNode(IRemediationsFprSource fprSource, ApplyResult result, Set<String> issueIdFilter);

    @Override
    public final boolean isSingular() { return true; }
}
