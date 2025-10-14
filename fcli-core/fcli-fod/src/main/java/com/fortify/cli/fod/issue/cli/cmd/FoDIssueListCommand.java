/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.fod.issue.cli.cmd;

import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.query.FoDFiltersParamGenerator;
import com.fortify.cli.fod._common.rest.query.cli.mixin.FoDFiltersParamMixin;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueEmbedMixin;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueIncludeMixin;
import com.fortify.cli.fod.issue.helper.FoDIssueHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FoDIssueListCommand extends AbstractFoDJsonNodeOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    private static final Log LOG = LogFactory.getLog(FoDIssueListCommand.class);

    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDAppResolverMixin.OptionalOption appResolver;
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.OptionalOption releaseResolver;
    @Mixin private FoDFiltersParamMixin filterParamMixin;
    @Mixin private FoDIssueEmbedMixin embedMixin;
    @Mixin private FoDIssueIncludeMixin includeMixin;
    @Getter private final IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new FoDFiltersParamGenerator()
            .add("id","id")
            .add("vulnId","vulnId")
            .add("instanceId","instanceId")
            .add("scanType","scanType")
            .add("status","status")
            .add("developerStatus","developerStatus")
            .add("auditorStatus","auditorStatus")
            .add("severity","severity")
            .add("severityString","severityString")
            .add("category","category");

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        // Get any server-side filters
        String filtersParamValue = Optional.ofNullable(filterParamMixin.getFilterExpression())
                .map(serverSideQueryParamGenerator::getServerSideQueryParamValue)
                .filter(v -> !v.isEmpty())
                .orElse(Optional.ofNullable(filterParamMixin.getServerSideQueryParamOptionValue()).orElse(""));
        // If a release is specified, just get issues for that release
        if (releaseResolver.getQualifiedReleaseNameOrId() != null) {
            if (appResolver.getAppNameOrId() != null) {
                throw new FcliSimpleException("Cannot specify both an application and release");
            }
            // If a release is specified, just get issues for that release
            result.addAll(FoDIssueHelper.getReleaseIssues(unirest, releaseResolver.getReleaseId(unirest),
                    includeMixin,
                    embedMixin,
                    filtersParamValue,
                    true));
            // call mergeReleaseIssues to ensure consistent ordering and deduplication of issues
            return FoDIssueHelper.mergeReleaseIssues(result);
        }
        // If an application is specified, get issues for all releases of that application
        if (appResolver.getAppNameOrId() != null) {
            // If an application is specified, get issues for all releases of that application
            List<String> releases = FoDReleaseHelper.getAllReleaseIdsForApp(unirest, appResolver.getAppId(unirest), true);
            if (releases.isEmpty()) {
                throw new FcliSimpleException("No releases found for application " + appResolver.getAppNameOrId());
            }
            for (String release : releases) {
                result.addAll(FoDIssueHelper.getReleaseIssues(unirest, release,
                        includeMixin,
                        embedMixin,
                        filtersParamValue,
                        true));
            }
            // call mergeReleaseIssues to ensure consistent ordering
            return FoDIssueHelper.mergeReleaseIssues(result);
        } else {
            throw new FcliSimpleException("Either an application or release must be specified");
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
