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
package com.fortify.cli.aviator.fpr.remediation.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fortify.cli.aviator.fpr.remediation.exception.SkipRemediationException;

public final class Remediation {
    private final String instanceId;
    private final List<FileChange> fileChanges;
    private final List<String> coveredByInstanceIds;
    private final List<String> supersedesInstanceIds;

    public Remediation(String instanceId, List<FileChange> fileChanges) {
        this(instanceId, fileChanges, List.of(), List.of());
    }

    public Remediation(String instanceId, List<FileChange> fileChanges, List<String> coveredByInstanceIds) {
        this(instanceId, fileChanges, coveredByInstanceIds, List.of());
    }

    public Remediation(String instanceId, List<FileChange> fileChanges, List<String> coveredByInstanceIds,
            List<String> supersedesInstanceIds) {
        this.instanceId = instanceId;
        this.fileChanges = fileChanges;
        this.coveredByInstanceIds = coveredByInstanceIds == null ? List.of() : List.copyOf(coveredByInstanceIds);
        this.supersedesInstanceIds = supersedesInstanceIds == null ? List.of() : List.copyOf(supersedesInstanceIds);
    }

    /** Issue ids whose fix already covers or contains this issue (this issue has no file changes of its own). */
    public List<String> coveredByInstanceIds() {
        return coveredByInstanceIds;
    }

    /** Issue ids whose own fix is contained in this remediation's file changes. */
    public List<String> supersedesInstanceIds() {
        return supersedesInstanceIds;
    }

    public boolean isCoveredByPriorFix() {
        return fileChanges.isEmpty() && !coveredByInstanceIds.isEmpty();
    }

    public String instanceId() {
        return instanceId;
    }

    public List<FileChange> fileChanges() {
        return fileChanges;
    }

    /**
     * Widest hunk (lineTo - lineFrom) across all FileChanges/Hunks in this remediation. Used to
     * order remediations broader-first so nested narrower fixes classify as SUPERSEDED.
     */
    public int maxHunkWidth() {
        int max = 0;
        for (FileChange fileChange : fileChanges) {
            for (Hunk hunk : fileChange.hunks()) {
                try {
                    int from = hunk.lineFrom();
                    int to = hunk.lineTo();
                    max = Math.max(max, to - from);
                } catch (SkipRemediationException ignore) {
                    // best-effort ordering; malformed hunks fall to the back
                }
            }
        }
        return max;
    }

    public List<RemediationKey> createRemediationKeys(Path sourceBasePath) {
        List<RemediationKey> keys = new ArrayList<>();
        for (FileChange fileChange : fileChanges) {
            for (Hunk hunk : fileChange.hunks()) {
                String filename = fileChange.requiredFilename();
                String comparisonCode = hunk.comparisonCode(filename);
                keys.add(RemediationKey.of(fileChange, hunk, sourceBasePath, comparisonCode));
            }
        }
        return keys;
    }
}
