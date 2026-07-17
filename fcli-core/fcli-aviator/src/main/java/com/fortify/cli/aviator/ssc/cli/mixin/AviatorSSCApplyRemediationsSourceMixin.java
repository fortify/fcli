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

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

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
        private OnlineSource online;

        @Option(names = {"--from-cache"}, required = true, paramLabel = "<zip>",
                descriptionKey = "fcli.aviator.ssc.apply-remediations.from-cache")
        private Path fromCache;
    }

    @Getter
    public static class OnlineSource {
        @ArgGroup(exclusive = true, multiplicity = "1")
        private OnlineModeArgGroup mode;

        @Option(names = {"--since"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.since")
        private String since;

        @Option(names = {"--appversion", "--av"}, descriptionKey = "fcli.ssc.appversion.resolver.nameOrId")
        private String appVersionNameOrId;

        @Option(names = {"--delim"}, defaultValue = ":")
        private String delimiter;
    }

    @Getter
    public static class OnlineModeArgGroup {
        @Option(names = {"--artifact-id"}, required = true, descriptionKey = "fcli.aviator.ssc.apply-remediations.artifact-id")
        private String artifactId;

        @Option(names = {"--latest"}, required = true, descriptionKey = "fcli.aviator.ssc.apply-remediations.latest")
        private boolean latest;

        @Option(names = {"--all"}, required = true, descriptionKey = "fcli.aviator.ssc.apply-remediations.all")
        private boolean all;
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
        return isOnlineSelected() && source.online.mode != null
                && StringUtils.isNotBlank(source.online.mode.artifactId);
    }

    public boolean isLatestSelected() {
        return isOnlineSelected() && source.online.mode != null && source.online.mode.latest;
    }

    public boolean isAllSelected() {
        return isOnlineSelected() && source.online.mode != null && source.online.mode.all;
    }

    public String getArtifactId() {
        return isArtifactIdSelected() ? source.online.mode.artifactId : null;
    }

    public String getSince() {
        return isOnlineSelected() ? source.online.since : null;
    }

    public String getAppVersionNameOrId() {
        return isOnlineSelected() ? source.online.appVersionNameOrId : null;
    }

    public String getAppVersionId(UnirestInstance unirest) {
        String appVersionNameOrId = getAppVersionNameOrId();
        if (StringUtils.isBlank(appVersionNameOrId)) {
            return null;
        }
        String delimiter = source.online.delimiter != null ? source.online.delimiter : ":";
        SSCAppVersionDescriptor descriptor = SSCAppVersionHelper.getRequiredAppVersion(
                unirest, appVersionNameOrId, delimiter, "id");
        return descriptor.getVersionId();
    }

    public void validate() {
        if (isFromCacheSelected()) {
            return;
        }
        if (!isOnlineSelected()) {
            throw new FcliSimpleException("Exactly one of --from-cache or online selection (--artifact-id, --latest, --all) must be specified");
        }
        if (StringUtils.isNotBlank(getSince()) && isArtifactIdSelected()) {
            throw new FcliSimpleException("--since cannot be used with --artifact-id; use --latest or --all");
        }
        if ((isLatestSelected() || isAllSelected()) && StringUtils.isBlank(getAppVersionNameOrId())) {
            throw new FcliSimpleException("--av/--appversion is required when using --latest or --all");
        }
        if (isArtifactIdSelected() && StringUtils.isNotBlank(getAppVersionNameOrId())) {
            throw new FcliSimpleException("--av/--appversion cannot be used with --artifact-id");
        }
    }
}
