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

    public Remediation(String instanceId, List<FileChange> fileChanges) {
        this.instanceId = instanceId;
        this.fileChanges = fileChanges;
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
