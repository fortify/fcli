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
import com.fortify.cli.common.cli.mixin.CommonOptionMixins.RequireConfirmation;

/**
 * Asks the user, through the standard fcli confirmation ({@code -y/--confirm} or an interactive prompt), whether a fix
 * that was not requested may be applied because it is the only way to resolve a requested issue. A fix that is not
 * confirmed is simply not applied: the issues that need it are skipped, with a reason, and the rest of the run continues.
 * The prompt text comes from the command's {@code confirmPrompt} message.
 */
public final class ConsoleImplicitFixApprover implements ImplicitFixApprover {
    private final RequireConfirmation confirmation;

    public ConsoleImplicitFixApprover(RequireConfirmation confirmation) {
        this.confirmation = confirmation;
    }

    @Override
    public Set<String> approve(List<ImplicitFix> proposals) {
        Set<String> approved = new LinkedHashSet<>();
        for (ImplicitFix proposal : proposals) {
            if (confirmation.isConfirmed(String.join(", ", proposal.requestedIssueIds()), proposal.fixId(),
                    String.join(", ", proposal.resolvedIssueIds()))) {
                approved.add(proposal.fixId());
            }
        }
        return approved;
    }
}
