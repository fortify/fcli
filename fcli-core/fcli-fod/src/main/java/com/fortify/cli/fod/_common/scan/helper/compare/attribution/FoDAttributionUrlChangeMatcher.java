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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeIndex.SiteTreeUrl;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeIndex.UrlMatch;

/**
 * Matches DAST issue locations against site tree URL changes. For URLs that exist in both site trees
 * but with different methods or status codes, the issue request method and response status code are
 * looked up (if a request lookup is available), to determine whether the change affects the issue.
 */
public final class FoDAttributionUrlChangeMatcher implements IFoDIssueEvidenceMatcher {
    @Override
    public List<FoDIssueEvidence> match(FoDAttributionContext context, FoDAttributionIssue issue) {
        var index = context.getChangeIndex(FoDAttributionUrlChangeIndex.class);
        if ( index == null ) { return List.of(); }
        var matches = index.lookup(issue.getLocation());
        if ( matches.isEmpty() ) { return matchRelated(context, index, issue); }
        var result = new ArrayList<FoDIssueEvidence>();
        for ( var match : matches ) {
            switch ( match.kind() ) {
                case ADDED -> result.add(evidence(context, match, FoDAttributionCause.URL_ADDED,
                        String.format("URL %s was added to the site tree", match.url())));
                case REMOVED -> result.add(evidence(context, match, FoDAttributionCause.URL_REMOVED,
                        String.format("URL %s was removed from the site tree", match.url())));
                case CHANGED -> result.addAll(matchChanged(context, issue, match));
                case UNCHANGED -> { /* No evidence */ }
            }
        }
        return result;
    }

    private static List<FoDIssueEvidence> matchRelated(FoDAttributionContext context, FoDAttributionUrlChangeIndex index,
            FoDAttributionIssue issue) {
        var related = index.findRelatedChanges(issue.getLocation());
        if ( related.isEmpty() ) { return List.of(); }
        var first = related.get(0);
        return List.of(evidence(context, first, FoDAttributionCause.RELATED_URL_CHANGED, String.format(
                "issue URL not found in site trees; %d related URL(s) changed, for example %s (%s)",
                related.size(), first.url(), first.kind().name().toLowerCase(Locale.ROOT))));
    }

    private static List<FoDIssueEvidence> matchChanged(FoDAttributionContext context, FoDAttributionIssue issue, UrlMatch match) {
        var lookup = context.getRequestLookup();
        var requestInfo = lookup == null ? null : lookup.lookup(issue);
        if ( requestInfo == null ) {
            return List.of(evidence(context, match, FoDAttributionCause.URL_CHANGED, String.format(
                    "URL %s changed: methods %s -> %s, status codes %s -> %s", match.url(),
                    match.previous().methods(), match.current().methods(),
                    match.previous().statusCodes(), match.current().statusCodes())));
        }
        // Fixed issues refer to the previous scan, so check whether the issue request is still observed in the
        // current scan; new and reopened issues refer to the current scan, so check the previous scan
        var isFixed = issue.transition() == FoDAttributionTransition.FIXED;
        var other = isFixed ? match.current() : match.previous();
        var otherScan = isFixed ? "current" : "previous";
        var result = new ArrayList<FoDIssueEvidence>();
        if ( StringUtils.isNotBlank(requestInfo.method()) && !containsIgnoreCase(other, requestInfo.method()) ) {
            result.add(evidence(context, match, FoDAttributionCause.URL_METHOD_CHANGED, String.format(
                    "issue request method %s for URL %s was not observed in %s scan (methods: %s)",
                    requestInfo.method(), match.url(), otherScan, other.methods())));
        }
        if ( StringUtils.isNotBlank(requestInfo.statusCode()) && !other.statusCodes().contains(requestInfo.statusCode()) ) {
            result.add(evidence(context, match, FoDAttributionCause.URL_STATUS_CHANGED, String.format(
                    "issue response status %s for URL %s was not observed in %s scan (status codes: %s)",
                    requestInfo.statusCode(), match.url(), otherScan, other.statusCodes())));
        }
        return result;
    }

    private static boolean containsIgnoreCase(SiteTreeUrl url, String method) {
        return url.methods().stream().anyMatch(method::equalsIgnoreCase);
    }

    private static FoDIssueEvidence evidence(FoDAttributionContext context, UrlMatch match, FoDAttributionCause cause, String evidence) {
        context.referenceChange(match.url());
        return new FoDIssueEvidence(cause, evidence);
    }
}
