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
package com.fortify.cli.fod.aviator.cli.mixin;

import java.nio.file.Path;

import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.cli.mixin.IFoDDelimiterMixinAware;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Source selection for FoD apply-remediations: online release or local remediations cache zip.
 * Standalone mixin (aligned with SSC apply source mixin layout).
 */
public final class FoDAviatorApplyRemediationsSourceMixin implements IFoDDelimiterMixinAware {
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

    @Getter
    static class SourceArgGroup {
        @ArgGroup(exclusive = false, multiplicity = "1")
        private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption online =
            new FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption();

        /** Shared description key: command-local option on an ArgGroup (default picocli key would use FQCN). */
        @Option(names = {"--from-cache"}, required = true, paramLabel = "<zip>",
                descriptionKey = "fcli.fod.aviator.apply-remediations.from-cache")
        private Path fromCache;
    }
}
