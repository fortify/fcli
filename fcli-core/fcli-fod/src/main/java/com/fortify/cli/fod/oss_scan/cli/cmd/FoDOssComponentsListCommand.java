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
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-components", aliases = "lsc")
@CommandGroup("oss-components")
public final class FoDOssComponentsListCommand extends AbstractFoDJsonNodeOutputCommand {
    private static final Logger LOG = LoggerFactory.getLogger(FoDOssComponentsListCommand.class);
    @Getter
    @Mixin
    private OutputHelperMixins.TableWithQuery outputHelper;
    @Mixin
    private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin
    private FoDAppResolverMixin.OptionalOption appResolver;
    @Mixin
    private FoDReleaseByQualifiedNameOrIdResolverMixin.OptionalOption releaseResolver;
    @Option(names = "--scan-types", required = true, split = ",", defaultValue = "Debricked")
    private FoDOpenSourceScanType[] scanTypes;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        Stream.of(scanTypes)
                .map(t -> getForOpenSourceScanType(unirest, t, releaseResolver.getReleaseId(unirest),
                        appResolver.getAppId(unirest), false))
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
            LOG.error("Error retrieving OSS components for release " + releaseResolver.getReleaseId(unirest)
                    + " and scan type " + scanType.name() + ": " + e.getMessage());
            return JsonHelper.getObjectMapper().createArrayNode();
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
