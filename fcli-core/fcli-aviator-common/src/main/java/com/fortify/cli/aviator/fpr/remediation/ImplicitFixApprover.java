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
package com.fortify.cli.aviator.fpr.remediation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides whether fixes that were not requested but are needed to resolve requested issues may be applied.
 * Example: issue D has no fix of its own because the wider fix for issue A supersedes it, so requesting only D
 * requires applying A (and with it everything else A resolves).
 */
@FunctionalInterface
public interface ImplicitFixApprover {
    /** @return ids of the proposed fixes that may be applied; proposals not returned are declined */
    Set<String> approve(List<ImplicitFix> proposals);

    /**
     * @param fixId the fix that would be applied although it was not requested
     * @param requestedIssueIds the requested issues that can only be resolved through this fix
     * @param resolvedIssueIds every issue (including the requested ones) that applying this fix resolves
     */
    record ImplicitFix(String fixId, Set<String> requestedIssueIds, Set<String> resolvedIssueIds) {}

    static ImplicitFixApprover approveAll() {
        return proposals -> proposals.stream().map(ImplicitFix::fixId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static ImplicitFixApprover denyAll() {
        return proposals -> Set.of();
    }
}
