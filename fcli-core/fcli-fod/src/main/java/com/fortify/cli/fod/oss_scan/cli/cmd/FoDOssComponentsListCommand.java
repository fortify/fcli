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
package com.fortify.cli.fod.oss_scan.cli.cmd;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod._common.rest.helper.FoDPagingHelper;
import com.fortify.cli.fod._common.scan.helper.FoDOpenSourceScanType;
import com.fortify.cli.fod._common.scan.helper.oss.FoDScanOssHelper;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-components", aliases = "lsc")
@CommandGroup("oss-components")
public final class FoDOssComponentsListCommand extends AbstractFoDJsonNodeOutputCommand {
    private static final Logger LOG = LoggerFactory.getLogger(FoDOssComponentsListCommand.class);
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin;
    @ArgGroup(exclusive = true, multiplicity = "1", order = 1) @Getter private TargetSpecifierArgGroup targetSpecifier = new TargetSpecifierArgGroup();
    @Option(names = "--scan-types", required = true, split = ",", defaultValue = "Debricked") private FoDOpenSourceScanType[] scanTypes;

    public static class TargetSpecifierArgGroup {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) @Getter private AppTarget app = new AppTarget();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) @Getter private ReleaseTarget release = new ReleaseTarget();
    }

    public static class AppTarget extends FoDAppResolverMixin.AbstractFoDAppResolverMixin {
        @Option(names = { "--app" }, required = true, descriptionKey = "fcli.fod.app.app-name-or-id") @Getter private String appNameOrId;
    }

    public static class ReleaseTarget extends FoDReleaseByQualifiedNameOrIdResolverMixin.AbstractFoDQualifiedReleaseNameOrIdResolverMixin {
        @Option(names = { "--release", "--rel" }, required = true, paramLabel = "id|app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.name-or-id") @Getter private String qualifiedReleaseNameOrId;
    }

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();

        var appGroup = targetSpecifier.getApp();
        var releaseGroup = targetSpecifier.getRelease();

        final String applicationId = (appGroup != null && appGroup.getAppNameOrId() != null)
                ? appGroup.getAppId(unirest)
                : null;
        final String releaseId = (releaseGroup != null && releaseGroup.getQualifiedReleaseNameOrId() != null)
                ? releaseGroup.getReleaseId(unirest)
                : null;

        Stream.of(scanTypes)
                .map(t -> getForOpenSourceScanType(unirest, t, releaseId, applicationId, false))
                .forEach(result::addAll);
        return result;
    }

    private ArrayNode getForOpenSourceScanType(UnirestInstance unirest, FoDOpenSourceScanType scanType,
            String releaseId, String applicationId, boolean failOnError) {
        LOG.debug(applicationId != null
                ? "Retrieving OSS components for application " + applicationId + " and scan type " + scanType.name()
                : "Retrieving OSS components for release " + releaseId + " and scan type " + scanType.name());
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        Map<String, Object> queryParams = new java.util.HashMap<>();
        if (applicationId != null) {
            queryParams.put("filters", "applicationId:" + applicationId);
        }
        if (releaseId != null) {
            queryParams.put("filters", "releaseId:" + releaseId);
        }
        queryParams.put("openSourceScanType", scanType.name());
        try {
            List<JsonNode> results = FoDPagingHelper.pagedRequest(unirest.get(FoDUrls.OSS_COMPONENTS)
                    .queryString(queryParams))
                    .stream()
                    .map(HttpResponse::getBody)
                    .map(FoDInputTransformer::getItems)
                    .map(ArrayNode.class::cast)
                    .flatMap(JsonHelper::stream)
                    .collect(Collectors.toList());
            for (JsonNode record : results) {
                result.add(FoDScanOssHelper.formatResults(record));
            }
            return result;
        } catch (UnexpectedHttpResponseException e) {
            if (failOnError) {
                throw e;
            }
            LOG.error("Error retrieving OSS components for release " + releaseId
                    + " and scan type " + scanType.name() + ": " + e.getMessage());
            return JsonHelper.getObjectMapper().createArrayNode();
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
