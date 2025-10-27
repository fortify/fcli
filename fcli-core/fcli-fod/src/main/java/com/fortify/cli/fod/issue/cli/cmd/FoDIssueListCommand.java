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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.producer.AbstractObjectNodeProducer.AbstractObjectNodeProducerBuilder;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.json.producer.SimpleObjectNodeProducer;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.query.FoDFiltersParamGenerator;
import com.fortify.cli.fod._common.rest.query.cli.mixin.FoDFiltersParamMixin;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueEmbedMixin;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueIncludeMixin;
import com.fortify.cli.fod.issue.helper.FoDIssueHelper;
import com.fortify.cli.fod.issue.helper.FoDIssueHelper.IssueAggregationData;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FoDIssueListCommand extends AbstractFoDOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // injected in resolvers
    @Mixin private FoDAppResolverMixin.OptionalOption appResolver;
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.OptionalOption releaseResolver;
    @Mixin private FoDFiltersParamMixin filterParamMixin;
    @Mixin private FoDIssueEmbedMixin embedMixin;
    @Mixin private FoDIssueIncludeMixin includeMixin;
    @Option(names="--aggregate", defaultValue="false") private boolean aggregate;
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
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirest) {
        boolean releaseSpecified = releaseResolver.getQualifiedReleaseNameOrId() != null;
        boolean appSpecified = appResolver.getAppNameOrId() != null;
        if ( releaseSpecified && appSpecified ) {
            throw new FcliSimpleException("Cannot specify both an application and release");
        }
        if ( !releaseSpecified && !appSpecified ) {
            throw new FcliSimpleException("Either an application or release must be specified");
        }
        var result = releaseSpecified
                ? singleReleaseProducerBuilder(unirest, releaseResolver.getReleaseId(unirest))
                : applicationProducerBuilder(unirest, appResolver.getAppId(unirest));
        // For consistent output, we should remove releaseId/releaseName when listing across multiple releases,
        // but that breaks existing scripts that may rely on those fields, so for now, we only do this in
        // applicationProducerBuilder(). TODO: Change in in fcli v4.0.
        // return result.recordTransformer(this::removeReleaseProperties).build();
        return result.build();
    }
    
    /**
     * Build a streaming producer for a single release. Uses requestObjectNodeProducerBuilder(SPEC)
     * to benefit from paging & transformations; server-side filtering is applied via
     * {@link FoDFiltersParamMixin} acting as an {@code IHttpRequestUpdater} when the builder applies SPEC.
     *
     * Enrichments added per record:
     * <ul>
     *   <li>releaseName (looked up once)</li>
     *   <li>issueUrl (browser convenience)</li>
     *   <li>Embed data if --embed specified</li>
     * </ul>
     * Record transformations from SPEC (query filtering etc) still apply after enrichment.
     *
     * @param unirest FoD REST client
     * @param releaseId Selected release id
     * @return Producer streaming transformed issue records for the release
     */
    private AbstractObjectNodeProducerBuilder<?,?> singleReleaseProducerBuilder(UnirestInstance unirest, String releaseId) {
        return releaseIssuesProducerBuilder(unirest, releaseId);
    }

    /**
     * Build a producer that lists merged issues across all releases for an application. We aggregate
     * issues per release first (reusing existing helper logic), then perform merge & stream results.
     * Streaming begins once all releases have been processed (merging requires full set).
     *
     * The merge operation combines issues with identical instanceId across releases, adding fields:
     * vulnIds|vulnIdsString, foundInReleases|foundInReleasesString, foundInReleaseIds|foundInReleaseIdsString,
     * ids|idsString. Ordering is applied by helper (severity desc, category, releaseId).
     *
     * NOTE: We currently need all issues loaded before streaming because merge logic correlates
     * across releases. Future optimization could stream partially if helper supports incremental merging.
     *
     * Server-side filters are computed per release using the same logic as the single-release path,
     * but we must pass the resulting string explicitly to {@link FoDIssueHelper#getReleaseIssues}.
     *
     * @param unirest FoD REST client
     * @param appId Application identifier
     * @return Producer streaming merged issue records (empty if no releases)
     */
    private AbstractObjectNodeProducerBuilder<?,?> applicationProducerBuilder(UnirestInstance unirest, String appId) {
        var result = isEffectiveFastOutput() 
                ? fastApplicationProducerBuilder(unirest, appId) 
                : mergedApplicationProducerBuilder(unirest, appId);
        return result.recordTransformer(this::removeReleaseProperties);
    }

    /** Fast streaming producer for application issues: sequentially streams issues from all releases without merging, de-duplicating on instanceId. */
    private AbstractObjectNodeProducerBuilder<?,?> fastApplicationProducerBuilder(UnirestInstance unirest, String appId) {
        List<String> releaseIds = loadReleaseIdsForApp(unirest, appId);
        if ( releaseIds.isEmpty() ) { return SimpleObjectNodeProducer.builder(); }
        Supplier<Stream<ObjectNode>> streamSupplier = () -> {
            Set<String> seenInstanceIds = new HashSet<>();
            return releaseIds.stream().flatMap(releaseId -> {
                List<ObjectNode> list = new ArrayList<>();
                releaseIssuesProducerBuilder(unirest, releaseId).build().forEach(node -> {
                    if ( node instanceof ObjectNode ) {
                        ObjectNode o = (ObjectNode)node;
                        String instanceId = o.has("instanceId") ? o.get("instanceId").asText() : null;
                        if ( instanceId != null && seenInstanceIds.add(instanceId) ) {
                            FoDIssueHelper.transformRecord(o, IssueAggregationData.blank());
                            list.add(o);
                        }
                    }
                    return Break.FALSE;
                });
                return list.stream();
            });
        };
        return streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .streamSupplier(streamSupplier);
    }

    /** Merged application producer; loads all issues then performs merge. */
    private AbstractObjectNodeProducerBuilder<?,?> mergedApplicationProducerBuilder(UnirestInstance unirest, String appId) {
        List<String> releaseIds = loadReleaseIdsForApp(unirest, appId);
        if ( releaseIds.isEmpty() ) { return SimpleObjectNodeProducer.builder(); }
        ArrayNode aggregated = JsonHelper.getObjectMapper().createArrayNode();
        for ( String releaseId : releaseIds ) {
            releaseIssuesProducerBuilder(unirest, releaseId).build()
                .forEach(node -> { aggregated.add(node); return Break.FALSE; });
        }
        return simpleObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .source(FoDIssueHelper.mergeReleaseIssues(aggregated));
    }

    /** Load release ids for given application using requestObjectNodeProducerBuilder(PRODUCT) for paging/product transformations. */
    private List<String> loadReleaseIdsForApp(UnirestInstance unirest, String appId) {
        List<String> releaseIds = new ArrayList<>();
        var producer = requestObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.PRODUCT)
                .baseRequest(unirest.get(FoDUrls.RELEASES).queryString("filters", "applicationId:"+appId))
                .build();
        producer.forEach(node -> {
            if ( node.has("releaseId") ) {
                releaseIds.add(node.get("releaseId").asText());
            }
            return Break.FALSE; // continue
        });
        return releaseIds;
    }

    @Override
    public boolean isSingular() { return false; }

    // Shared per-release issues producer builder
    private AbstractObjectNodeProducerBuilder<?,?> releaseIssuesProducerBuilder(UnirestInstance unirest, String releaseId) {
        FoDReleaseDescriptor releaseDescriptor = FoDReleaseHelper.getReleaseDescriptorFromId(unirest, Integer.parseInt(releaseId), true);
        String releaseName = releaseDescriptor.getReleaseName();
        HttpRequest<?> request = unirest.get(FoDUrls.VULNERABILITIES)
                .routeParam("relId", releaseId)
                .queryString("orderBy", "severity")
                .queryString("orderDirection", "ASC");
        return requestObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .baseRequest(request)
                .recordTransformer(n -> enrichIssueRecord(unirest, releaseName, n));
    }

    private JsonNode enrichIssueRecord(UnirestInstance unirest, String releaseName, JsonNode n) {
        if ( n instanceof ObjectNode node ) {
            node.put("releaseName", releaseName);
            node.put("issueUrl", FoDIssueHelper.getIssueUrl(unirest, node.get("id").asText()));
            IssueAggregationData data = isEffectiveFastOutput() 
                    ? IssueAggregationData.blank() 
                    : IssueAggregationData.forSingleRelease(node);
            FoDIssueHelper.transformRecord(node, data);
        }
        return n;
    }
    
    private boolean isEffectiveFastOutput() {
        boolean appSpecified = appResolver.getAppNameOrId() != null;
        boolean releaseSpecified = releaseResolver.getQualifiedReleaseNameOrId() != null;
        if ( !appSpecified || releaseSpecified ) { return false; }
        boolean fastOutputStyle = outputHelper.getRecordWriterStyle().isFastOutput();
        boolean streamingSupported = outputHelper.isStreamingOutputSupported();
        boolean recordConsumerConfigured = getRecordConsumer()!=null;
        // Effective fast output requires:
        // - application specified (multiple releases)
        // - fast output style
        // - no aggregation (merging requires full set)
        // - streaming output or record consumer configured
        return fastOutputStyle && !aggregate && (streamingSupported || recordConsumerConfigured);
    }
    
    private final JsonNode removeReleaseProperties(JsonNode n) {
        if ( n instanceof ObjectNode node ) {
            node.remove("releaseId");
            node.remove("releaseName");
        }
        return n;
    }
}
