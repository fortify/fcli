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
package com.fortify.cli.agent.extensions.helper;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks the state of an install/update plan for condition evaluation.
 * Used by the condition evaluator to resolve "installed" references.
 */
public final class AgentExtensionsInstallPlanContext {
    /**
     * Set of "assistantId/contentType" strings representing targets that are
     * planned for installation in the current run or were installed previously.
     */
    private final Set<String> installedTargets = new HashSet<>();

    /**
     * Set of resolved target directories that have already been processed,
     * used for auto-deduplication.
     */
    private final Set<String> processedTargetDirs = new HashSet<>();

    public void markInstalled(String assistantId, String contentType) {
        installedTargets.add(assistantId + "/" + contentType);
    }

    public boolean isInstalled(String ref) {
        return installedTargets.contains(ref);
    }

    /**
     * Mark a target directory + content type combination as processed.
     * @return true if this is a new combination (not yet processed), false if duplicate
     */
    public boolean markTargetDir(Path resolvedTargetDir, String contentType) {
        var key = resolvedTargetDir.toString() + ":" + contentType;
        return processedTargetDirs.add(key);
    }
}
