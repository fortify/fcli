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
package com.fortify.cli.ai_assist.extensions.helper;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks the state of an install/update plan for directory-overlap deduplication.
 * When multiple assistants share a target directory, only the first one installs;
 * subsequent assistants reuse the existing files (EXISTING).
 */
public final class AiAssistExtensionsInstallPlanContext {
    /**
     * Set of resolved target directory + content type combinations that have
     * already been covered (files installed or planned) by a previous assistant.
     */
    private final Set<Path> coveredDirs = new HashSet<>();

    /**
     * Mark a target directory as covered (files have been installed there).
     */
    public void markCovered(Path resolvedTargetDir, String contentType) {
        coveredDirs.add(toCoverageKey(resolvedTargetDir, contentType));
    }

    /**
     * Check if a target directory is already covered for a given content type.
     */
    public boolean isCovered(Path resolvedTargetDir, String contentType) {
        return coveredDirs.contains(toCoverageKey(resolvedTargetDir, contentType));
    }

    /**
     * Find the first covered directory from a list of candidates.
     * @return the first covered path, or null if none are covered
     */
    public Path findCoveredDir(List<Path> candidates, String contentType) {
        return candidates.stream()
            .filter(p -> isCovered(p, contentType))
            .findFirst()
            .orElse(null);
    }

    private static Path toCoverageKey(Path dir, String contentType) {
        return dir.resolve("__ct__" + contentType);
    }
}
