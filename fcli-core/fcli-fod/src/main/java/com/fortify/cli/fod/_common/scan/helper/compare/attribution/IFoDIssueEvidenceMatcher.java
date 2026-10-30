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

/**
 * Produces issue-specific evidence, like the issue location having been added, removed or changed.
 * Matchers don't need to check whether the evidence is relevant for the issue status transition;
 * evidence for causes that can't explain the issue's transition is ignored by the attribution engine.
 */
public interface IFoDIssueEvidenceMatcher {
    List<FoDIssueEvidence> match(FoDAttributionContext context, FoDAttributionIssue issue);
}
