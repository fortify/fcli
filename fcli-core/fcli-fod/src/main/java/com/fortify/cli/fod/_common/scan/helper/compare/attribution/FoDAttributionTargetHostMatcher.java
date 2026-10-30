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

import java.util.List;
import java.util.Objects;

/**
 * If the DAST target URL changed to a different host, issue correlation breaks; fixed issues on
 * the previous target host and new issues on the current target host are directly attributed.
 */
public final class FoDAttributionTargetHostMatcher implements IFoDIssueEvidenceMatcher {
    private final String targetUrlPointer;

    public FoDAttributionTargetHostMatcher(String targetUrlPointer) {
        this.targetUrlPointer = targetUrlPointer;
    }

    @Override
    public List<FoDIssueEvidence> match(FoDAttributionContext context, FoDAttributionIssue issue) {
        var previous = FoDAttributionUrls.normalize(context.getPreviousText(targetUrlPointer));
        var current = FoDAttributionUrls.normalize(context.getCurrentText(targetUrlPointer));
        var issueUrl = FoDAttributionUrls.normalize(issue.getLocation());
        if ( previous == null || current == null || issueUrl == null || issueUrl.origin() == null
                || previous.origin() == null || Objects.equals(previous.origin(), current.origin()) ) {
            return List.of();
        }
        var isFixed = issue.transition() == FoDAttributionTransition.FIXED;
        var target = isFixed ? previous : current;
        if ( !issueUrl.origin().equals(target.origin()) ) { return List.of(); }
        return List.of(new FoDIssueEvidence(FoDAttributionCause.TARGET_CHANGE, String.format(
                "issue host %s matches %s target URL; target URL changed from %s to %s",
                issueUrl.origin(), isFixed ? "previous" : "current", previous.key(), current.key())));
    }
}
