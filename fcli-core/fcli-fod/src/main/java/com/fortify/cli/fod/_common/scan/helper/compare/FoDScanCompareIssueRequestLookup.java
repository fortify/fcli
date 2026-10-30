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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDIssueRequestLookup;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDIssueRequestLookup.RequestInfo;

import kong.unirest.UnirestInstance;

/** Looks up the request method and response status code for DAST issues, based on the issue request/response */
final class FoDScanCompareIssueRequestLookup extends AbstractFoDScanCompareIssueLookup<RequestInfo>
        implements IFoDIssueRequestLookup {
    // If changed, update the 'fcli.fod.dast-scan.compare.usage.description.4' message in FoDMessages.properties
    static final int MAX_LOOKUPS = 250;

    FoDScanCompareIssueRequestLookup(UnirestInstance unirest, String releaseId) {
        super(unirest, releaseId, MAX_LOOKUPS, "request/response");
    }

    @Override
    protected RequestInfo load(String vulnId) {
        var body = unirest.get(FoDUrls.VULNERABILITY_REQUEST_RESPONSE)
                .routeParam("relId", releaseId).routeParam("vulnId", vulnId)
                .asObject(JsonNode.class).getBody();
        var requestResponse = body != null && body.isArray() ? body.path(0) : body;
        return requestResponse == null ? null : parse(requestResponse);
    }

    // Request content starts with '<method> <path> HTTP/x.y', response content with 'HTTP/x.y <status> <reason>'
    static RequestInfo parse(JsonNode requestResponse) {
        var method = getToken(requestResponse.path("requestContent").asText(""), 0);
        var statusCode = getToken(requestResponse.path("responseContent").asText(""), 1);
        return method == null && statusCode == null ? null : new RequestInfo(method, statusCode);
    }

    private static String getToken(String content, int index) {
        var firstLine = StringUtils.substringBefore(content, "\n").trim();
        var tokens = StringUtils.split(firstLine);
        return tokens != null && tokens.length > index ? tokens[index] : null;
    }
}
