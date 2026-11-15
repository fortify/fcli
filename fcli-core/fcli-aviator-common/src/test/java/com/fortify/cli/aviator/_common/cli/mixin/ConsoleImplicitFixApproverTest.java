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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.remediation.ImplicitFixApprover.ImplicitFix;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins.RequireConfirmation;

class ConsoleImplicitFixApproverTest {
    /** Records what the user would have been asked and confirms only the given fixes. */
    private static final class FakeConfirmation extends RequireConfirmation {
        private final Set<String> confirmedFixIds;
        private final List<List<Object>> asked = new ArrayList<>();

        FakeConfirmation(String... confirmedFixIds) {
            this.confirmedFixIds = Set.of(confirmedFixIds);
        }

        @Override
        public boolean isConfirmed(Object... promptArgs) {
            asked.add(List.of(promptArgs));
            return confirmedFixIds.contains(promptArgs[1]);
        }
    }

    private static ImplicitFix proposal(String fixId, Set<String> requested, Set<String> resolved) {
        return new ImplicitFix(fixId, requested, resolved);
    }

    @Test
    void approvesOnlyTheFixesThatWereConfirmed() {
        FakeConfirmation confirmation = new FakeConfirmation("fix-A");
        Set<String> approved = new ConsoleImplicitFixApprover(confirmation).approve(List.of(
            proposal("fix-A", Set.of("D"), Set.of("D")),
            proposal("fix-B", Set.of("E"), Set.of("E"))));

        assertEquals(Set.of("fix-A"), approved);
        assertEquals(2, confirmation.asked.size());
    }

    @Test
    void approvesNothingWhenNothingIsConfirmed() {
        Set<String> approved = new ConsoleImplicitFixApprover(new FakeConfirmation()).approve(List.of(
            proposal("fix-A", Set.of("D"), Set.of("D"))));

        assertTrue(approved.isEmpty());
    }

    @Test
    void asksNothingWhenThereAreNoProposals() {
        FakeConfirmation confirmation = new FakeConfirmation("fix-A");

        assertTrue(new ConsoleImplicitFixApprover(confirmation).approve(List.of()).isEmpty());
        assertTrue(confirmation.asked.isEmpty());
    }

    @Test
    void promptReceivesTheRequestedIssuesTheFixAndEverythingItResolves() {
        FakeConfirmation confirmation = new FakeConfirmation("fix-A");
        Set<String> requested = new LinkedHashSet<>(List.of("D1", "D2"));
        Set<String> resolved = new LinkedHashSet<>(List.of("D1", "D2", "X"));

        new ConsoleImplicitFixApprover(confirmation).approve(List.of(proposal("fix-A", requested, resolved)));

        assertEquals(List.of(List.of("D1, D2", "fix-A", "D1, D2, X")), confirmation.asked);
    }
}
