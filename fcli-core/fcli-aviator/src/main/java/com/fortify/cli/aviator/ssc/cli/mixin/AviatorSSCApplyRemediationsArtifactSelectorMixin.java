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

/**
 * Mixin for selecting which artifact(s) to process for apply-remediations command.
 * Uses Picocli ArgGroups to enforce mutually exclusive options.
 */
@Getter
public class AviatorSSCApplyRemediationsArtifactSelectorMixin {

    @ArgGroup(exclusive = true, multiplicity = "1")
    private ArtifactSelectionArgGroup artifactSelection;

    @Option(names = {"--since"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.since")
    private String since;

    // Options needed by --latest and --all-open-issues (not by --artifact-id)
    @Option(names = {"--appversion", "--av"}, descriptionKey = "fcli.ssc.appversion.resolver.nameOrId")
    private String appVersionNameOrId;

    @Option(names = {"--delim"}, defaultValue = ":")
    private String delimiter;

    @Getter
    public static class ArtifactSelectionArgGroup {
        @Option(names = {"--artifact-id"}, required = true, descriptionKey = "fcli.aviator.ssc.apply-remediations.artifact-id")
        private String artifactId;

        @Option(names = {"--latest"}, required = true, descriptionKey = "fcli.aviator.ssc.apply-remediations.latest")
        private boolean latest;

        @Option(names = {"--all"}, required = true, descriptionKey = "fcli.aviator.ssc.apply-remediations.all")
        private boolean allOpenIssues;
    }

    public boolean isArtifactIdSelected() {
        return artifactSelection != null && StringUtils.isNotBlank(artifactSelection.artifactId);
    }

    public boolean isLatestSelected() {
        return artifactSelection != null && artifactSelection.latest;
    }

    public boolean isAllOpenIssuesSelected() {
        return artifactSelection != null && artifactSelection.allOpenIssues;
    }

    public String getArtifactId() {
        return isArtifactIdSelected() ? artifactSelection.artifactId : null;
    }

    public String getAppVersionNameOrId() {
        return appVersionNameOrId;
    }

    public String getAppVersionId(UnirestInstance unirest) {
        if (StringUtils.isBlank(appVersionNameOrId)) {
            return null;
        }
        SSCAppVersionDescriptor descriptor = SSCAppVersionHelper.getRequiredAppVersion(
            unirest, appVersionNameOrId, delimiter, "id");
        return descriptor.getVersionId();
    }

    public void validate() {
        // Validate --since is only used with --latest or --all-open-issues
        if (since != null && !since.isBlank() && isArtifactIdSelected()) {
            throw new FcliSimpleException(
                "--since cannot be used with --artifact-id; use --latest or --all-open-issues");
        }

        // Validate --av is required with --latest or --all-open-issues
        if ((isLatestSelected() || isAllOpenIssuesSelected()) && StringUtils.isBlank(appVersionNameOrId)) {
            throw new FcliSimpleException(
                "--av/--appversion is required when using --latest or --all-open-issues");
        }

        // Validate --av is not used with --artifact-id
        if (isArtifactIdSelected() && StringUtils.isNotBlank(appVersionNameOrId)) {
            throw new FcliSimpleException(
                "--av/--appversion cannot be used with --artifact-id");
        }
    }
}
