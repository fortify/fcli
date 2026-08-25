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
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorApplyRemediationsCommand;
import com.fortify.cli.aviator._common.remediations_cache.CacheRemediationsFprSource;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCApplyRemediationsSourceMixin;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsSelectorArgGroups.OnlineSelectionArgGroup.ResolvedOnlineArtifacts;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCApplyRemediationsHelper;
import com.fortify.cli.aviator.ssc.helper.SSCOnlineRemediationsFprSource;
import com.fortify.cli.aviator.ssc.helper.SinceOptionHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.cli.mixin.SSCUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "apply-remediations")
public class AviatorSSCApplyRemediationsCommand extends AbstractAviatorApplyRemediationsCommand {
    @Mixin private AviatorSSCApplyRemediationsSourceMixin sourceSelector;
    @Mixin private SSCUnirestInstanceSupplierMixin unirestInstanceSupplier;

    @Override
    protected void validateSourceSelector() {
        sourceSelector.validate();
    }

    @Override
    protected boolean isFromCacheSelected() {
        return sourceSelector.isFromCacheSelected();
    }

    @Override
    protected JsonNode processFromCache(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        try (CacheRemediationsFprSource source = CacheRemediationsFprSource.open(
                sourceSelector.getFromCache(), RemediationsCacheConstants.PRODUCT_SSC)) {
            ApplyResult applyResult = RemediationsApplyHelper.apply(
                    source, applyOptions, issueIdFilter, logger);
            return AviatorSSCApplyRemediationsHelper.buildCacheResultNode(
                    sourceSelector.getFromCache(), applyResult, issueIdFilter,
                    source.reader().getManifest().getSelection());
        }
    }

    @Override
    protected JsonNode processOnline(
            AviatorLoggerImpl logger, IProgressWriter progressWriter, Set<String> issueIdFilter) {
        UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
        OffsetDateTime sinceDate = SinceOptionHelper.parse(sourceSelector.getOnline().getSince());
        // One resolve: artifacts + appVersionId (no second getAppVersionId REST call).
        ResolvedOnlineArtifacts resolved = sourceSelector.getOnline().resolveArtifacts(unirest, sinceDate);
        try (SSCOnlineRemediationsFprSource source = new SSCOnlineRemediationsFprSource(
                unirest, logger, progressWriter, resolved.artifacts())) {
            ApplyResult applyResult = RemediationsApplyHelper.apply(
                    source, applyOptions, issueIdFilter, logger);
            return AviatorSSCApplyRemediationsHelper.buildOnlineResultNode(
                    resolved.artifacts(), resolved.appVersionId(), applyResult, issueIdFilter);
        }
    }
}
