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

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

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

        /** Shared description from module Messages.properties ({@code delim=}). */
        @Option(names = {"--delim"}, defaultValue = ":", descriptionKey = "delim")
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

        /**
         * Selected online mode, or {@code null} if none (should not occur after validate /
         * exclusive ArgGroup). Used for manifest {@code selection.mode} wire values.
         */
        public SSCRemediationsSelectionMode getSelectionMode() {
            if (isArtifactIdSelected()) {
                return SSCRemediationsSelectionMode.ARTIFACT_ID;
            }
            if (isLatestSelected()) {
                return SSCRemediationsSelectionMode.LATEST;
            }
            if (isAllSelected()) {
                return SSCRemediationsSelectionMode.ALL;
            }
            return null;
        }

        /**
         * Resolves online selection once: artifacts plus appVersionId for {@code --latest}/{@code --all}.
         * Shared by download-remediations-cache and apply-remediations so selection behavior stays aligned.
         * {@code --artifact-id} is validated with {@link SSCArtifactHelper#requireAviatorArtifact};
         * appVersionId is null in that mode (callers may read it from the artifact JSON if needed).
         */
        public ResolvedOnlineArtifacts resolveArtifacts(UnirestInstance unirest, OffsetDateTime sinceDate) {
            if (isAllSelected()) {
                String appVersionId = getAppVersionId(unirest);
                return new ResolvedOnlineArtifacts(
                        SSCArtifactHelper.getAllAviatorArtifacts(unirest, appVersionId, sinceDate),
                        appVersionId);
            }
            if (isLatestSelected()) {
                String appVersionId = getAppVersionId(unirest);
                return new ResolvedOnlineArtifacts(
                        List.of(SSCArtifactHelper.getLatestAviatorArtifact(unirest, appVersionId, sinceDate)),
                        appVersionId);
            }
            FcliSimpleException.throwIf(!isArtifactIdSelected(),
                    "Exactly one of --artifact-id, --latest, or --all must be specified");
            return new ResolvedOnlineArtifacts(
                    List.of(SSCArtifactHelper.requireAviatorArtifact(
                            SSCArtifactHelper.getArtifactDescriptor(unirest, getArtifactId()))),
                    null);
        }

        /** Result of {@link #resolveArtifacts}: one REST resolve of app version when applicable. */
        public record ResolvedOnlineArtifacts(List<SSCArtifactDescriptor> artifacts, String appVersionId) {}

        public void validate() {
            FcliSimpleException.throwIf(StringUtils.isNotBlank(since) && isArtifactIdSelected(),
                    "--since cannot be used with --artifact-id; use --latest or --all");
            FcliSimpleException.throwIf(
                    (isLatestSelected() || isAllSelected()) && StringUtils.isBlank(appVersionNameOrId),
                    "--av/--appversion is required when using --latest or --all");
            FcliSimpleException.throwIf(isArtifactIdSelected() && StringUtils.isNotBlank(appVersionNameOrId),
                    "--av/--appversion cannot be used with --artifact-id");
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
