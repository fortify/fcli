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

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorApplyRemediationsCommand;
import com.fortify.cli.aviator._common.remediations_cache.CacheRemediationsFprSource;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.aviator.cli.mixin.FoDAviatorApplyRemediationsSourceMixin;
import com.fortify.cli.fod.aviator.helper.AviatorFoDApplyRemediationsHelper;
import com.fortify.cli.fod.aviator.helper.FoDOnlineRemediationsFprSource;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "apply-remediations")
public class FoDAviatorApplyRemediationsCommand extends AbstractAviatorApplyRemediationsCommand {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Injected into sourceSelector
    @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Mixin private FoDAviatorApplyRemediationsSourceMixin sourceSelector;

    @Override
    protected boolean isFromCacheSelected() {
        return sourceSelector.isFromCacheSelected();
    }

    @Override
    protected JsonNode processFromCache(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        try (CacheRemediationsFprSource source = CacheRemediationsFprSource.open(
                sourceSelector.getFromCache(), RemediationsCacheConstants.PRODUCT_FOD)) {
            ApplyResult applyResult = RemediationsApplyHelper.apply(
                    source, applyOptions, issueIdFilter, logger);
            return AviatorFoDApplyRemediationsHelper.buildCacheResultNode(
                    sourceSelector.getFromCache(), applyResult, issueIdFilter);
        }
    }

    @Override
    protected JsonNode processOnline(
            AviatorLoggerImpl logger, IProgressWriter progressWriter, Set<String> issueIdFilter) {
        UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
        FoDReleaseDescriptor release = sourceSelector.getReleaseDescriptor(unirest);
        try (FoDOnlineRemediationsFprSource source =
                new FoDOnlineRemediationsFprSource(unirest, logger, release)) {
            ApplyResult applyResult = RemediationsApplyHelper.apply(
                    source, applyOptions, issueIdFilter, logger);
            return AviatorFoDApplyRemediationsHelper.buildOnlineResultNode(release, applyResult);
        }
    }
}
