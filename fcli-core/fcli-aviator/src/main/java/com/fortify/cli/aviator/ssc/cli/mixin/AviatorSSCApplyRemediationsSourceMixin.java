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
import java.time.OffsetDateTime;

import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsSelectorArgGroups.OnlineSelectionArgGroup;
import com.fortify.cli.common.exception.FcliSimpleException;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Source selection for apply-remediations: either online SSC selection or a local remediations cache zip.
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
                descriptionKey = "fcli.aviator.ssc.apply-remediations.from-cache")
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

    public boolean isArtifactIdSelected() {
        return isOnlineSelected() && source.online.isArtifactIdSelected();
    }

    public boolean isLatestSelected() {
        return isOnlineSelected() && source.online.isLatestSelected();
    }

    public boolean isAllSelected() {
        return isOnlineSelected() && source.online.isAllSelected();
    }

    public String getArtifactId() {
        return isOnlineSelected() ? source.online.getArtifactId() : null;
    }

    public String getSince() {
        return isOnlineSelected() ? source.online.getSince() : null;
    }

    public String getAppVersionNameOrId() {
        return isOnlineSelected() ? source.online.getAppVersionNameOrId() : null;
    }

    public String getAppVersionId(UnirestInstance unirest) {
        return isOnlineSelected() ? source.online.getAppVersionId(unirest) : null;
    }

    /** Online only; same resolution as download-remediations-cache. */
    public OnlineSelectionArgGroup.ResolvedOnlineArtifacts resolveArtifacts(
            UnirestInstance unirest, OffsetDateTime sinceDate) {
        FcliSimpleException.throwIf(!isOnlineSelected(),
                "Online artifact selection is required (not --from-cache)");
        return source.online.resolveArtifacts(unirest, sinceDate);
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
