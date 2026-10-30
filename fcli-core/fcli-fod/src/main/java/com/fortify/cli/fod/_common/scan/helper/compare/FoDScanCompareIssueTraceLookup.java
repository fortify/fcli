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
package com.fortify.cli.fod._common.scan.helper.compare;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDIssueTrace;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDIssueTrace.Node;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDIssueTraceLookup;

import kong.unirest.UnirestInstance;

/** Looks up the dataflow traces for SAST issues */
final class FoDScanCompareIssueTraceLookup extends AbstractFoDScanCompareIssueLookup<List<FoDIssueTrace>>
        implements IFoDIssueTraceLookup {
    // If changed, update the 'fcli.fod.sast-scan.compare.usage.description.4' message in FoDMessages.properties
    static final int MAX_LOOKUPS = 250;

    FoDScanCompareIssueTraceLookup(UnirestInstance unirest, String releaseId) {
        super(unirest, releaseId, MAX_LOOKUPS, "traces");
    }

    @Override
    protected List<FoDIssueTrace> load(String vulnId) {
        var body = unirest.get(FoDUrls.VULNERABILITY_TRACES)
                .routeParam("relId", releaseId).routeParam("vulnId", vulnId)
                .asObject(JsonNode.class).getBody();
        return body == null ? null : parse(body);
    }

    // Response is an array of traces, each with an array of trace entries ordered from source to sink. Entries without
    // location are retained, such that source and sink are always the first and last entry.
    static List<FoDIssueTrace> parse(JsonNode body) {
        var traces = body.isArray() ? body : body.path("items");
        if ( !traces.isArray() ) { return null; }
        var result = new ArrayList<FoDIssueTrace>();
        for ( var trace : traces ) {
            var nodes = new ArrayList<Node>();
            for ( var entry : trace.path("traceEntries") ) {
                var lineNumber = entry.path("lineNumber");
                nodes.add(new Node(StringUtils.trimToNull(entry.path("location").asText(null)),
                        lineNumber.isNumber() ? lineNumber.asInt() : null));
            }
            if ( nodes.stream().anyMatch(Node::hasLocation) ) { result.add(new FoDIssueTrace(nodes)); }
        }
        return result;
    }
}
