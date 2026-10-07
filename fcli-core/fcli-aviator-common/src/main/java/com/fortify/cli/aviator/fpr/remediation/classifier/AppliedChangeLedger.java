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
 * Per-FPR offset ledger, NOT shared across artifacts in multi-FPR runs (e.g., --all-open-issues).
 * Each RemediationProcessor gets a fresh instance isolated to that artifact's scan.
 *
 * <p>Line numbers in AppliedChange are expressed in pristine-file coordinates relative to that
 * FPR's audit. Different artifacts are scans of potentially different source revisions, so
 * sharing a ledger across artifacts would incorrectly project deltas from one coordinate system
 * onto another. Anchor verification (hash checking) is the safety mechanism for multi-FPR runs:
 * once an earlier FPR has touched a file, the declared hash no longer matches, so every later
 * hunk is written only where its OriginalCode still literally matches. Phase 3 (server-side
 * re-audit on code change) is the fundamental fix for multi-FPR consistency.
 *
 * <p>Populated when a hunk is written to disk; consulted before applying subsequent hunks to
 * detect SUPERSEDED/CONFLICTS and to project declared line ranges through prior edits within
 * the same FPR.
 */
public final class AppliedChangeLedger {
    private final Map<Path, List<AppliedChange>> appliedByFile = new LinkedHashMap<>();
    private final List<PendingAppliedChange> pendingAppliedChanges = new ArrayList<>();

    /**
     * Committed changes for a file, plus any changes staged so far by the remediation currently
     * being processed (not yet committed). Hunks of the same remediation are applied one at a
     * time in a single pass ({@code FileWriteCoordinator.processFileChanges}), so a later hunk's
     * offset projection must be able to see the shift an earlier hunk in that same remediation
     * already staged, not just changes committed by prior remediations.
     */
    public List<AppliedChange> changesFor(Path filePath) {
        List<AppliedChange> committed = appliedByFile.getOrDefault(filePath, List.of());
        List<AppliedChange> staged = pendingAppliedChanges.stream()
            .filter(pac -> pac.filePath().equals(filePath))
            .map(AppliedChangeLedger::fromPending)
            .toList();
        if (staged.isEmpty()) {
            return committed;
        }
        List<AppliedChange> combined = new ArrayList<>(committed);
        combined.addAll(staged);
        return combined;
    }

    /** Stage a hunk this remediation intends to apply. Merged into the ledger on {@link #commitStaged()}. */
    public void stage(PendingAppliedChange pendingAppliedChange) {
        pendingAppliedChanges.add(pendingAppliedChange);
    }

    /** Discards all currently staged entries (skip/rollback of the whole remediation). */
    public void discardStaged() {
        pendingAppliedChanges.clear();
    }

    /** Moves all staged entries into the committed ledger (called only after a successful write) and clears staging. */
    public void commitStaged() {
        for (PendingAppliedChange pac : pendingAppliedChanges) {
            appliedByFile.computeIfAbsent(pac.filePath(), k -> new ArrayList<>())
                .add(fromPending(pac));
        }
        pendingAppliedChanges.clear();
    }

    private static AppliedChange fromPending(PendingAppliedChange pac) {
        return AppliedChange.builder()
            .originalLineFrom(pac.lineFrom())
            .originalLineTo(pac.lineTo())
            .declaredLineFrom(pac.declaredLineFrom())
            .declaredLineTo(pac.declaredLineTo())
            .deltaLines(pac.deltaLines())
            .comparisonCode(pac.comparisonCode())
            .lineNormalizedCode(pac.lineNormalizedCode())
            .build();
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
