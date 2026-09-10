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
package com.fortify.cli.aviator.fpr.remediation.classifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fortify.cli.aviator.fpr.remediation.model.AppliedChange;

/**
 * Per-run offset ledger. Replaces the two raw fields ({@code appliedByFile},
 * {@code pendingAppliedChanges}) that used to live directly on the orchestrator, giving the
 * stage -&gt; commit-or-discard lifecycle one explicit, independently-testable home.
 *
 * <p>Populated when a hunk is written to disk; consulted before applying subsequent hunks to
 * detect SUPERSEDED/CONFLICTS and to project declared line ranges through prior edits. A fresh
 * instance is used per apply operation (there is no reset method), matching the original's
 * per-{@code RemediationProcessor}-instance lifetime.
 */
public final class AppliedChangeLedger {
    private final Map<Path, List<AppliedChange>> appliedByFile = new LinkedHashMap<>();
    private final List<PendingAppliedChange> pendingAppliedChanges = new ArrayList<>();

    /** Committed changes for a file, or an empty list if none have been applied yet this run. */
    public List<AppliedChange> changesFor(Path filePath) {
        return appliedByFile.getOrDefault(filePath, List.of());
    }

    /** Stage a hunk this remediation intends to apply. Merged into the ledger on {@link #commitStaged()}. */
    public void stage(PendingAppliedChange pendingAppliedChange) {
        pendingAppliedChanges.add(pendingAppliedChange);
    }

    /** Current staging-list size, to be passed back to {@link #discardStagedSince(int)} for a partial rollback. */
    public int stagedMark() {
        return pendingAppliedChanges.size();
    }

    /** Discards only the staged entries added since {@code mark} (a single file's failed staging), keeping earlier ones. */
    public void discardStagedSince(int mark) {
        while (pendingAppliedChanges.size() > mark) {
            pendingAppliedChanges.remove(pendingAppliedChanges.size() - 1);
        }
    }

    /** Discards all currently staged entries (skip/rollback of the whole remediation). */
    public void discardStaged() {
        pendingAppliedChanges.clear();
    }

    /** Moves all staged entries into the committed ledger (called only after a successful write) and clears staging. */
    public void commitStaged() {
        for (PendingAppliedChange pac : pendingAppliedChanges) {
            appliedByFile.computeIfAbsent(pac.filePath(), k -> new ArrayList<>())
                .add(new AppliedChange(pac.instanceId(), pac.lineFrom(), pac.lineTo(), pac.deltaLines(), pac.comparisonCode()));
        }
        pendingAppliedChanges.clear();
    }

    /**
     * Accumulated line-delta shift for {@code filePath} from every committed change whose
     * original range sits strictly before {@code lineFrom}.
     */
    public int projectOffset(Path filePath, int lineFrom) {
        int shift = 0;
        for (AppliedChange ac : changesFor(filePath)) {
            if (ac.originalLineTo() < lineFrom) {
                shift += ac.deltaLines();
            }
        }
        return shift;
    }
}
