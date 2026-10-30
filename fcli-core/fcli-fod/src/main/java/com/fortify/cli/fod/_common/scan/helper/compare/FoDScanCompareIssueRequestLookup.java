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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionIssue;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDIssueRequestLookup;

import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Looks up the request method and response status code for DAST issues, based on the issue
 * request/response. Results are cached per issue, and the number of lookups is limited to
 * bound the number of API calls.
 */
@Slf4j
final class FoDScanCompareIssueRequestLookup implements IFoDIssueRequestLookup {
    static final int MAX_LOOKUPS = 250;
    private final UnirestInstance unirest;
    private final String releaseId;
    private final Map<String, RequestInfo> cache = new HashMap<>();
    @Getter private int lookupCount;
    @Getter private boolean limitReached;

    FoDScanCompareIssueRequestLookup(UnirestInstance unirest, String releaseId) {
        this.unirest = unirest;
        this.releaseId = releaseId;
    }

    @Override
    public RequestInfo lookup(FoDAttributionIssue issue) {
        var vulnId = issue.getText("vulnId");
        if ( StringUtils.isBlank(vulnId) ) { return null; }
        if ( cache.containsKey(vulnId) ) { return cache.get(vulnId); }
        if ( lookupCount >= MAX_LOOKUPS ) {
            limitReached = true;
            return null;
        }
        lookupCount++;
        var result = load(vulnId);
        cache.put(vulnId, result);
        return result;
    }

    // Request data is optional evidence, so any failure results in the issue being attributed without it
    private RequestInfo load(String vulnId) {
        try {
            var body = unirest.get(FoDUrls.VULNERABILITY_REQUEST_RESPONSE)
                    .routeParam("relId", releaseId).routeParam("vulnId", vulnId)
                    .asObject(JsonNode.class).getBody();
            var requestResponse = body != null && body.isArray() ? body.path(0) : body;
            return requestResponse == null ? null : parse(requestResponse);
        } catch ( UnexpectedHttpResponseException e ) {
            log.debug("No request/response available for issue {} (HTTP status {})", vulnId, e.getStatus());
            return null;
        } catch ( UnirestException e ) {
            log.debug("Error loading request/response for issue {}", vulnId, e);
            return null;
        }
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
