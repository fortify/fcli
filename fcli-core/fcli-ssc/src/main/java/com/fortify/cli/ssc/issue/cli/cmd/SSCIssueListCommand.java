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
package com.fortify.cli.ssc.issue.cli.cmd;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.query.SSCQParamGenerator;
import com.fortify.cli.ssc._common.rest.ssc.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc._common.rest.ssc.query.cli.mixin.SSCQParamMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueBulkEmbedMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueIncludeMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCIssueListCommand extends AbstractSSCOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @Mixin private SSCIssueFilterSetResolverMixin.FilterSetOption filterSetResolver;
    @Mixin private SSCQParamMixin qParamMixin;
    @Mixin private SSCIssueBulkEmbedMixin bulkEmbedMixin;
    @Option(names="--filter", required=false) private String filter;
    @Mixin private SSCIssueIncludeMixin includeMixin;
    
    // For some reason, SSC q param doesn't use same property names as returned by SSC,
    // so we list the proper mappings below. TODO Any other useful server-side queries?
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
        .add("issueName", "category", SSCQParamValueGenerators::wrapInQuotes)
        .add("fullFileName", "file", SSCQParamValueGenerators::wrapInQuotes);

    @Override
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirest) {
        String appVersionId = parentResolver.getAppVersionId(unirest);
        SSCIssueFilterSetDescriptor filterSetDescriptor = filterSetResolver.getFilterSetDescriptor(unirest, appVersionId);
        Map<String, String> folderNameByGuid = getFolderNameByGuid(filterSetDescriptor);
    return requestObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .baseRequest(getBaseRequest(unirest, appVersionId, filterSetDescriptor))
                .recordTransformer(n -> addFolderName(n, folderNameByGuid))
                .build();
    }
    
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest, String appVersionId, SSCIssueFilterSetDescriptor filterSetDescriptor) {
        GetRequest request = unirest.get("/api/v1/projectVersions/{id}/issues?limit=100&qm=issues")
                .routeParam("id", appVersionId);
        if ( filterSetDescriptor!=null ) {
            request.queryString("filterset", filterSetDescriptor.getGuid());
        }
        if ( filter!=null ) {
            request.queryString("filter", new SSCIssueFilterHelper(unirest, appVersionId).getFilter(filter));
        }
        return request;
    }
    
    private Map<String, String> getFolderNameByGuid(SSCIssueFilterSetDescriptor descriptor) {
        if ( descriptor==null || descriptor.getFolders()==null || descriptor.getFolders().isEmpty() ) { 
            return Collections.emptyMap(); 
        }
        Map<String, String> result = new HashMap<>();
        descriptor.getFolders().forEach(f -> {
            var name = f.getName();
            var guid = f.getGuid();
            if ( guid!=null && !guid.isBlank() ) {
                result.put(guid, name); // name may be null; we allow mapping to null explicitly
            }
        });
        return result.isEmpty() ? Collections.emptyMap() : result;
    }
    
    private JsonNode addFolderName(JsonNode n, Map<String, String> folderNameByGuid) {
        if ( n==null || !n.isObject() ) { return n; }
        ObjectNode on = (ObjectNode)n;
        String guid = textValue(on, "folderGuid");
        String name = guid==null ? null : folderNameByGuid.get(guid);
        on.put("folderName", name);
        return on;
    }
    
    private String textValue(JsonNode n, String... names) {
        for ( String name : names ) {
            JsonNode v = n.get(name);
            if ( v!=null && v.isValueNode() ) { String t = v.asText(); if ( t!=null && !t.isBlank() ) { return t; } }
        }
        return null;
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
