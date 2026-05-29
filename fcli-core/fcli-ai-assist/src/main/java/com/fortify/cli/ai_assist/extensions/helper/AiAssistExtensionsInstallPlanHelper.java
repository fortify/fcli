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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fortify.cli.common.exception.FcliSimpleException;

/**
 * Builds and executes idempotent install/update plans for AI assistant extensions.
 * A plan describes which files to install, update, remove, or leave unchanged.
 */
final class AiAssistExtensionsInstallPlanHelper {
    private AiAssistExtensionsInstallPlanHelper() {}

    record PlanEntry(
        String assistant, String assistantId, String contentType,
        String targetDir, String sourceFile, String targetRelPath,
        String targetAbsPath, String sourceVersion, String action) {}

    /**
     * Tracks directory-overlap deduplication during plan building.
     * When multiple assistants share a target directory, only the first one
     * installs; subsequent assistants reuse the existing files (EXISTING).
     */
    static final class PlanContext {
        private final Set<Path> coveredDirs = new HashSet<>();

        void markCovered(Path resolvedTargetDir, String contentType) {
            coveredDirs.add(toCoverageKey(resolvedTargetDir, contentType));
        }

        Path findCoveredDir(List<Path> candidates, String contentType) {
            return candidates.stream()
                .filter(p -> coveredDirs.contains(toCoverageKey(p, contentType)))
                .findFirst()
                .orElse(null);
        }

        private static Path toCoverageKey(Path dir, String contentType) {
            return dir.resolve("__ct__" + contentType);
        }
    }

    // ──────────────────────────── Setup plan (assistant-based) ────────────────────────────

    static List<PlanEntry> buildSetupPlan(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsDistributionDescriptor distribution,
            Map<String, AiAssistExtensionsAssistantDescriptor> assistants,
            AiAssistExtensionsSourceHandler sourceHandler,
            PlanContext planContext,
            Set<String> contentTypeFilter,
            String sourceVersion) {
        var plan = new ArrayList<PlanEntry>();

        for (var entry : assistants.entrySet()) {
            var assistantId = entry.getKey();
            var assistant = entry.getValue();
            if (assistant.getTargets() == null) { continue; }

            for (var target : assistant.getTargets()) {
                var contentType = target.getContentType();
                if (!AiAssistExtensionsStateHelper.matchesContentTypeFilter(contentType, contentTypeFilter)) { continue; }

                var resolvedDirs = AiAssistExtensionsPathResolver.resolveAll(target.getTargetDirs());
                if (resolvedDirs.isEmpty()) { continue; }

                var coveredDir = planContext.findCoveredDir(resolvedDirs, contentType);
                boolean isExisting = coveredDir != null;
                var resolvedDir = isExisting ? coveredDir : resolvedDirs.get(0);

                if (!isExisting) {
                    planContext.markCovered(resolvedDir, contentType);
                }

                if (isExisting) {
                    addExistingEntries(plan, assistant, assistantId, contentType,
                        resolvedDir, contentManifest, target, sourceHandler, sourceVersion);
                    continue;
                }

                addDiffEntries(plan, assistant.getDisplayName(), assistantId, contentType,
                    resolvedDir, contentManifest, target, sourceHandler, sourceVersion);
            }
        }
        return plan;
    }

    private static void addExistingEntries(
            List<PlanEntry> plan,
            AiAssistExtensionsAssistantDescriptor assistant, String assistantId,
            String contentType, Path resolvedDir,
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsTargetDescriptor target,
            AiAssistExtensionsSourceHandler sourceHandler, String sourceVersion) {
        var sourceFiles = AiAssistExtensionsContentHelper.discoverSourceFiles(contentManifest, target, sourceHandler);
        for (var sourceFile : sourceFiles) {
            var targetRelPath = AiAssistExtensionsContentHelper.getTargetRelativePath(contentManifest, target, sourceFile);
            plan.add(new PlanEntry(
                assistant.getDisplayName(), assistantId, contentType,
                resolvedDir.toString(), sourceFile, targetRelPath,
                AiAssistExtensionsStateHelper.safeResolve(resolvedDir, targetRelPath).toString(),
                sourceVersion, "EXISTING"));
        }
    }

    private static void addDiffEntries(
            List<PlanEntry> plan, String displayName, String assistantId,
            String contentType, Path resolvedDir,
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsTargetDescriptor target,
            AiAssistExtensionsSourceHandler sourceHandler, String sourceVersion) {
        var existingManifest = AiAssistExtensionsStateHelper.readTargetDirManifest(resolvedDir, contentType);
        var existingFiles = existingManifest != null && existingManifest.getFiles() != null
            ? new HashSet<>(existingManifest.getFiles()) : Set.<String>of();

        var sourceFiles = AiAssistExtensionsContentHelper.discoverSourceFiles(contentManifest, target, sourceHandler);
        var handledRelPaths = new HashSet<String>();
        for (var sourceFile : sourceFiles) {
            var targetRelPath = AiAssistExtensionsContentHelper.getTargetRelativePath(contentManifest, target, sourceFile);
            var targetAbsPath = AiAssistExtensionsStateHelper.safeResolve(resolvedDir, targetRelPath).toString();
            handledRelPaths.add(targetRelPath);

            String action;
            if (!existingFiles.contains(targetRelPath)) {
                action = "INSTALLED";
            } else if (AiAssistExtensionsStateHelper.hasFileChanged(sourceHandler, sourceFile, Path.of(targetAbsPath))) {
                action = "UPDATED";
            } else {
                action = "UNCHANGED";
            }
            plan.add(new PlanEntry(
                displayName, assistantId, contentType,
                resolvedDir.toString(), sourceFile, targetRelPath,
                targetAbsPath, sourceVersion, action));
        }

        for (var existingFile : existingFiles) {
            if (!handledRelPaths.contains(existingFile)) {
                plan.add(new PlanEntry(
                    displayName, assistantId, contentType,
                    resolvedDir.toString(), null, existingFile,
                    AiAssistExtensionsStateHelper.safeResolve(resolvedDir, existingFile).toString(),
                    sourceVersion, "REMOVED"));
            }
        }
    }

    // ──────────────────────────── Custom-dir plan ────────────────────────────

    static List<PlanEntry> buildCustomDirPlan(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsSourceHandler sourceHandler,
            Set<String> contentTypeFilter, String customDir,
            String sourceVersion) {
        var plan = new ArrayList<PlanEntry>();
        var resolvedDir = Path.of(customDir).toAbsolutePath().normalize();
        if (contentManifest.getContentTypes() == null) { return plan; }

        for (var ctEntry : contentManifest.getContentTypes().entrySet()) {
            var contentType = ctEntry.getKey();
            if (!AiAssistExtensionsStateHelper.matchesContentTypeFilter(contentType, contentTypeFilter)) { continue; }

            var ctDesc = ctEntry.getValue();
            var existingManifest = AiAssistExtensionsStateHelper.readTargetDirManifest(resolvedDir, contentType);
            var existingFiles = existingManifest != null && existingManifest.getFiles() != null
                ? new HashSet<>(existingManifest.getFiles()) : Set.<String>of();

            var sourceFiles = AiAssistExtensionsContentHelper.discoverSourceFilesForContentType(ctDesc, sourceHandler);
            var handledRelPaths = new HashSet<String>();
            for (var sourceFile : sourceFiles) {
                var targetRelPath = AiAssistExtensionsContentHelper.getTargetRelativePathForContentType(ctDesc, sourceFile);
                var targetAbsPath = AiAssistExtensionsStateHelper.safeResolve(resolvedDir, targetRelPath).toString();
                handledRelPaths.add(targetRelPath);

                String action;
                if (!existingFiles.contains(targetRelPath)) {
                    action = "INSTALLED";
                } else if (AiAssistExtensionsStateHelper.hasFileChanged(sourceHandler, sourceFile, Path.of(targetAbsPath))) {
                    action = "UPDATED";
                } else {
                    action = "UNCHANGED";
                }
                plan.add(new PlanEntry(
                    null, null, contentType,
                    resolvedDir.toString(), sourceFile, targetRelPath,
                    targetAbsPath, sourceVersion, action));
            }

            for (var existingFile : existingFiles) {
                if (!handledRelPaths.contains(existingFile)) {
                    plan.add(new PlanEntry(
                        null, null, contentType,
                        resolvedDir.toString(), null, existingFile,
                        AiAssistExtensionsStateHelper.safeResolve(resolvedDir, existingFile).toString(),
                        sourceVersion, "REMOVED"));
                }
            }
        }
        return plan;
    }

    // ──────────────────────────── Plan execution ────────────────────────────

    static void executePlan(List<PlanEntry> plan,
            AiAssistExtensionsSourceHandler sourceHandler) {
        var byDirAndType = plan.stream()
            .filter(e -> !"EXISTING".equals(e.action()))
            .collect(Collectors.groupingBy(
                e -> e.targetDir() + "\0" + e.contentType(),
                LinkedHashMap::new, Collectors.toList()));

        for (var dirEntries : byDirAndType.values()) {
            for (var entry : dirEntries) {
                switch (entry.action()) {
                    case "INSTALLED", "UPDATED" ->
                        AiAssistExtensionsStateHelper.installFile(sourceHandler,
                            entry.sourceFile(), Path.of(entry.targetAbsPath()));
                    case "REMOVED" ->
                        AiAssistExtensionsStateHelper.deleteTargetFile(Path.of(entry.targetAbsPath()));
                }
            }

            var first = dirEntries.get(0);
            var installedFiles = dirEntries.stream()
                .filter(e -> !"REMOVED".equals(e.action()))
                .map(PlanEntry::targetRelPath)
                .toList();
            AiAssistExtensionsStateHelper.writeTargetDirManifest(Path.of(first.targetDir()),
                first.contentType(), first.sourceVersion(), installedFiles);
        }
    }

    // ──────────────────────────── Plan → output descriptors ────────────────────────────

    static List<AiAssistExtensionsOutputDescriptor> toOutputDescriptors(List<PlanEntry> plan) {
        var groups = plan.stream().collect(Collectors.groupingBy(
            e -> e.assistantId() + "\0" + e.contentType() + "\0" + e.targetDir() + "\0" + e.action(),
            LinkedHashMap::new, Collectors.toList()));

        var result = new ArrayList<AiAssistExtensionsOutputDescriptor>();
        for (var entries : groups.values()) {
            var first = entries.get(0);
            var files = entries.stream()
                .map(PlanEntry::targetRelPath)
                .toArray(String[]::new);
            result.add(AiAssistExtensionsOutputDescriptor.builder()
                .assistant(first.assistant())
                .assistantId(first.assistantId())
                .contentType(first.contentType())
                .targetDir(first.targetDir())
                .fileCount(files.length)
                .sourceVersion(first.sourceVersion())
                .files(files)
                .filesString(String.join(", ", files))
                .actionResult(first.action())
                .build());
        }
        return result;
    }

    // ──────────────────────────── Validation ────────────────────────────

    static void validatePlanHasTools(List<PlanEntry> plan) {
        if (plan.isEmpty()) {
            throw new FcliSimpleException("No content to install for the selected assistants/content types");
        }
    }
}
