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
package com.fortify.cli.aviator.ssc.cli.mixin;

import java.nio.file.Path;

import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsSelectorArgGroups.OnlineSelectionArgGroup;
import com.fortify.cli.common.cli.util.WindowsPathConverter;
import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Source selection for apply-remediations: either online SSC selection or a local remediations cache zip.
 * Online selection is the shared {@link OnlineSelectionArgGroup} (no pass-through accessors).
 */
@Getter
public class AviatorSSCApplyRemediationsSourceMixin {

    @ArgGroup(exclusive = true, multiplicity = "1")
    private SourceArgGroup source;

    @Getter
    public static class SourceArgGroup {
        @ArgGroup(exclusive = false)
        private OnlineSelectionArgGroup online;

        /** Shared/arg-group option: keep descriptionKey (default picocli key uses FQCN). */
        @Option(names = {"--from-cache"}, required = true, paramLabel = "<zip>",
                descriptionKey = "fcli.aviator.ssc.apply-remediations.from-cache",
                converter = WindowsPathConverter.class)
        private Path fromCache;
    }

    public boolean isFromCacheSelected() {
        return source != null && source.fromCache != null;
    }

    public boolean isOnlineSelected() {
        return source != null && source.online != null;
    }

    public Path getFromCache() {
        return isFromCacheSelected() ? source.fromCache : null;
    }

    /** Online ArgGroup when online mode is selected; null for --from-cache. */
    public OnlineSelectionArgGroup getOnline() {
        return isOnlineSelected() ? source.online : null;
    }

    public void validate() {
        if (isFromCacheSelected()) {
            return;
        }
        FcliSimpleException.throwIf(!isOnlineSelected(),
                "Exactly one of --from-cache or online selection (--artifact-id, --latest, --all) must be specified");
        source.online.validate();
    }
}
