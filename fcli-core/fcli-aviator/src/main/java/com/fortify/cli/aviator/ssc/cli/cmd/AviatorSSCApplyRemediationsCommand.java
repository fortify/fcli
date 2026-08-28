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
import com.fortify.cli.aviator._common.remediations_cache.IRemediationsFprSource;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCApplyRemediationsOptionsMixin;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsSelectorArgGroups.OnlineSelectionArgGroup.ResolvedOnlineArtifacts;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCApplyRemediationsHelper;
import com.fortify.cli.aviator.ssc.helper.SSCOnlineRemediationsFprSource;
import com.fortify.cli.aviator.ssc.helper.SinceOptionHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.cli.mixin.SSCUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;
import lombok.AccessLevel;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "apply-remediations")
public class AviatorSSCApplyRemediationsCommand extends AbstractAviatorApplyRemediationsCommand {
    @Getter(AccessLevel.PROTECTED) @Mixin private AviatorSSCApplyRemediationsOptionsMixin applyOptions;
    @Mixin private SSCUnirestInstanceSupplierMixin unirestInstanceSupplier;

    @Override
    protected IRemediationsFprSource openFprSource(AviatorLoggerImpl logger, IProgressWriter progressWriter) {
        return applyOptions.isFromCacheSelected()
                ? openCacheFprSource()
                : openOnlineFprSource(logger, progressWriter);
    }

    @Override
    protected JsonNode buildResultNode(IRemediationsFprSource fprSource, ApplyResult result, Set<String> issueIdFilter) {
        return applyOptions.isFromCacheSelected()
                ? buildCacheResultNode(fprSource, result, issueIdFilter)
                : buildOnlineResultNode(fprSource, result, issueIdFilter);
    }

    private IRemediationsFprSource openCacheFprSource() {
        return CacheRemediationsFprSource.open(
                applyOptions.getFromCache(),
                RemediationsCacheConstants.PRODUCT_SSC);
    }

    private IRemediationsFprSource openOnlineFprSource(AviatorLoggerImpl logger, IProgressWriter progressWriter) {
        UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
        OffsetDateTime sinceDate = SinceOptionHelper.parse(applyOptions.getOnline().getSince());
        ResolvedOnlineArtifacts resolvedOnline = applyOptions.getOnline().resolveArtifacts(unirest, sinceDate);
        return new SSCOnlineRemediationsFprSource(unirest, logger, progressWriter, resolvedOnline);
    }

    private JsonNode buildCacheResultNode(IRemediationsFprSource fprSource, ApplyResult result, Set<String> issueIdFilter) {
        return AviatorSSCApplyRemediationsHelper.buildCacheResultNode(
                applyOptions.getFromCache(), result, issueIdFilter,
                ((CacheRemediationsFprSource) fprSource).reader().getManifest().getSelection());
    }

    private JsonNode buildOnlineResultNode(IRemediationsFprSource fprSource, ApplyResult result, Set<String> issueIdFilter) {
        ResolvedOnlineArtifacts resolvedOnline = ((SSCOnlineRemediationsFprSource) fprSource).getResolvedOnline();
        return AviatorSSCApplyRemediationsHelper.buildOnlineResultNode(
                resolvedOnline.artifacts(), resolvedOnline.appVersionId(), result, issueIdFilter);
    }
}
