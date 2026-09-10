package com.fortify.cli.aviator.fpr.remediation.model;

/** Per-hunk classification for the state machine. */
public enum HunkOutcome {
    APPLIED, IDENTICAL, SUPERSEDED, CONFLICTS, POSSIBLY_REMEDIATED, ANCHOR_MISMATCH
}
