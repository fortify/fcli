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
import java.util.List;

import com.fortify.cli.aviator.fpr.remediation.exception.SkipRemediationException;
import com.fortify.cli.aviator.fpr.remediation.model.AppliedChange;
import com.fortify.cli.aviator.fpr.remediation.model.FileChange;
import com.fortify.cli.aviator.fpr.remediation.model.Hunk;
import com.fortify.cli.aviator.fpr.remediation.model.HunkOutcome;
import com.fortify.cli.aviator.fpr.remediation.model.Remediation;

/**
 * Pre-classifies each hunk of a {@link Remediation} against the per-run {@link AppliedChangeLedger}.
 * Returns {@link HunkOutcome#SUPERSEDED} if a prior applied hunk fully contains the range AND
 * its fix content actually covers this hunk's proposed change (normalized, comment/whitespace-
 * insensitive substring match); {@link HunkOutcome#POSSIBLY_REMEDIATED} for a fully-nested
 * range whose content does NOT match what was actually written (a different fix hidden behind
 * a broader one, but the location is still covered); {@link HunkOutcome#CONFLICTS} for a
 * partial, non-nested overlap (coverage is genuinely ambiguous); {@link HunkOutcome#APPLIED}
 * for no overlap (candidate to attempt). Identity-satisfied hunks whose exact range was written
 * by a prior remediation naturally classify as SUPERSEDED, which is semantically correct.
 */
public final class HunkClassifier {

    public List<HunkOutcome> classifyRemediationHunks(Remediation remediation, Path sourceBasePath, AppliedChangeLedger ledger) {
        List<HunkOutcome> outcomes = new ArrayList<>();
        for (FileChange fileChange : remediation.fileChanges()) {
            Path filePath;
            try {
                filePath = fileChange.resolve(sourceBasePath);
            } catch (SkipRemediationException e) {
                outcomes.add(HunkOutcome.APPLIED);
                continue;
            }
            List<AppliedChange> applied = ledger.changesFor(filePath);
            for (Hunk hunk : fileChange.hunks()) {
                int from;
                int to;
                try {
                    from = hunk.lineFrom();
                    to = hunk.lineTo();
                } catch (Exception e) {
                    outcomes.add(HunkOutcome.APPLIED);
                    continue;
                }
                String candidateComparisonCode = null;
                try {
                    candidateComparisonCode = hunk.comparisonCode(fileChange.requiredFilename());
                } catch (SkipRemediationException e) {
                    // Content unavailable for comparison; classifyRange falls back to range-only classification.
                }
                outcomes.add(classifyRange(from, to, applied, candidateComparisonCode));
            }
        }
        return outcomes;
    }

    /**
     * SUPERSEDED if nested in an AppliedChange whose written content actually covers this
     * hunk's proposed fix (normalized substring match); POSSIBLY_REMEDIATED if nested but the
     * content differs (the sibling fully covers this location, just not proven identical);
     * CONFLICTS if there is only a partial, non-nested line overlap (neither range contains
     * the other, so coverage is genuinely ambiguous); APPLIED otherwise. When either side's
     * content is unavailable ({@code null}), falls back to the conservative range-only default
     * of SUPERSEDED for a fully-nested range.
     */
    private HunkOutcome classifyRange(int lineFrom, int lineTo, List<AppliedChange> applied, String candidateComparisonCode) {
        for (AppliedChange ac : applied) {
            if (ac.coversFully(lineFrom, lineTo)) {
                if (ac.contentCovers(candidateComparisonCode)) {
                    return HunkOutcome.SUPERSEDED;
                }
                return HunkOutcome.POSSIBLY_REMEDIATED;
            }
            if (ac.overlapsPartially(lineFrom, lineTo)) {
                return HunkOutcome.CONFLICTS;
            }
        }
        return HunkOutcome.APPLIED;
    }
}
