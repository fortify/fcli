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
package com.fortify.cli.aviator._common.cli.mixin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fortify.cli.aviator.fpr.remediation.ImplicitFixApprover;

/**
 * Asks the user whether a fix that was not requested may be applied because it is the only way to resolve a requested
 * issue. {@code --confirm} approves without asking; without a console and without {@code --confirm} nothing is approved.
 */
public final class ConsoleImplicitFixApprover implements ImplicitFixApprover {
    private final boolean confirmed;

    public ConsoleImplicitFixApprover(boolean confirmed) {
        this.confirmed = confirmed;
    }

    @Override
    public Set<String> approve(List<ImplicitFix> proposals) {
        Set<String> approved = new LinkedHashSet<>();
        for (ImplicitFix proposal : proposals) {
            if (confirmed || askUser(proposal)) {
                approved.add(proposal.fixId());
            }
        }
        return approved;
    }

    private boolean askUser(ImplicitFix proposal) {
        if (System.console() == null) {
            return false;
        }
        Set<String> others = new LinkedHashSet<>(proposal.resolvedIssueIds());
        others.removeAll(proposal.requestedIssueIds());
        String prompt = String.format(
            "Issue(s) %s have no fix of their own; they are resolved by the wider fix for issue %s%s.%nApply the fix for issue %s? (y/N) ",
            String.join(", ", proposal.requestedIssueIds()), proposal.fixId(),
            others.isEmpty() ? "" : ", which also resolves " + String.join(", ", others), proposal.fixId());
        String response = System.console().readLine(prompt);
        return response != null && (response.trim().equalsIgnoreCase("y") || response.trim().equalsIgnoreCase("yes"));
    }
}
