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

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public final class AviatorSSCRemediationsSelectorArgGroups {
    private AviatorSSCRemediationsSelectorArgGroups() {}

    @Getter
    public static class OnlineSelectionArgGroup {
        @ArgGroup(exclusive = true, multiplicity = "1")
        private OnlineModeArgGroup mode;

        @Option(names = {"--since"}, descriptionKey = "fcli.aviator.ssc.remediations-cache.since")
        private String since;

        @Option(names = {"--appversion", "--av"}, descriptionKey = "fcli.ssc.appversion.resolver.nameOrId")
        private String appVersionNameOrId;

        @Option(names = {"--delim"}, defaultValue = ":")
        private String delimiter;

        public boolean isArtifactIdSelected() {
            return mode != null && StringUtils.isNotBlank(mode.artifactId);
        }

        public boolean isLatestSelected() {
            return mode != null && mode.latest;
        }

        public boolean isAllSelected() {
            return mode != null && mode.all;
        }

        public String getArtifactId() {
            return isArtifactIdSelected() ? mode.artifactId : null;
        }

        public String getAppVersionId(UnirestInstance unirest) {
            if (StringUtils.isBlank(appVersionNameOrId)) {
                return null;
            }
            SSCAppVersionDescriptor descriptor = SSCAppVersionHelper.getRequiredAppVersion(
                    unirest, appVersionNameOrId, getDelimiter(), "id");
            return descriptor.getVersionId();
        }

        public String getSelectionMode() {
            if (isArtifactIdSelected()) {
                return "artifact-id";
            }
            if (isLatestSelected()) {
                return "latest";
            }
            if (isAllSelected()) {
                return "all";
            }
            return null;
        }

        public void validate() {
            if (StringUtils.isNotBlank(since) && isArtifactIdSelected()) {
                throw new FcliSimpleException("--since cannot be used with --artifact-id; use --latest or --all");
            }
            if ((isLatestSelected() || isAllSelected()) && StringUtils.isBlank(appVersionNameOrId)) {
                throw new FcliSimpleException("--av/--appversion is required when using --latest or --all");
            }
            if (isArtifactIdSelected() && StringUtils.isNotBlank(appVersionNameOrId)) {
                throw new FcliSimpleException("--av/--appversion cannot be used with --artifact-id");
            }
        }
    }

    @Getter
    public static class OnlineModeArgGroup {
        @Option(names = {"--artifact-id"}, required = true, descriptionKey = "fcli.aviator.ssc.remediations-cache.artifact-id")
        private String artifactId;

        @Option(names = {"--latest"}, required = true, descriptionKey = "fcli.aviator.ssc.remediations-cache.latest")
        private boolean latest;

        @Option(names = {"--all"}, required = true, descriptionKey = "fcli.aviator.ssc.remediations-cache.all")
        private boolean all;
    }
}