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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsInstallationsDescriptor.AssistantInstallation;
import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsSourceHandler.DigestMismatchAction;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

/**
 * Core setup/uninstall/list logic for AI assistant extensions.
 * <p>
 * State is managed in two tiers:
 * <ul>
 *   <li><b>fcli state</b> ({@code state/ai-assist/extensions/installations.json}):
 *       lightweight registry of which assistants were set up and their resolved
 *       target directories. Used by {@code list-installed} and {@code uninstall}
 *       to work without the distribution descriptor.</li>
 *   <li><b>Target-dir manifest</b> ({@code .fortify-extensions.<contentType>.json} in each target dir):
 *       records content type, version, and file list. Enables diff-based updates
 *       and state recovery after fcli state reset.</li>
 * </ul>
 */
public final class AiAssistExtensionsInstaller {
    private static final Logger LOG = LoggerFactory.getLogger(AiAssistExtensionsInstaller.class);
    private static final Path INSTALLATIONS_STATE_PATH =
        Path.of("state", "ai-assist", "extensions", "installations.json");

    private AiAssistExtensionsInstaller() {}

    // ──────────────────────────── Version resolution ────────────────────────────

    public static ToolDefinitionRootDescriptor getToolDefinitions() {
        return ToolDefinitionsHelper.getToolDefinitionRootDescriptor(
            AiAssistExtensionsSourceHandler.TOOL_NAME);
    }

    public static ToolDefinitionVersionDescriptor resolveVersion(String version) {
        return getToolDefinitions().getVersionOrDefault(version);
    }

    // ──────────────────────────── Setup (idempotent install/update) ────────────────────────────

    /**
     * Idempotent setup: installs if new, updates if already present
     * (adds new files, updates changed files, removes obsolete files).
     *
     * @param source           local zip/dir override, or null for tool-definitions
     * @param version          version string (default "latest")
     * @param assistants       explicit assistant IDs, or null
     * @param autoDetect       true to auto-detect assistants
     * @param contentTypeFilter filter by content type, or null for all
     * @param customDir        custom target directory (mutually exclusive with assistants/autoDetect)
     * @param onDigestMismatch action on signature mismatch
     * @param dryRun           if true, plan only without executing
     */
    public static List<AiAssistExtensionsOutputDescriptor> setup(
            String source, String version,
            Set<String> assistants, boolean autoDetect,
            Set<String> contentTypeFilter, String customDir,
            DigestMismatchAction onDigestMismatch, boolean dryRun) {

        try (var sourceHandler = resolveSource(source, version, onDigestMismatch)) {
            var contentManifest = sourceHandler.readContentManifest();

            if (customDir != null) {
                // --dir mode: install content types directly to custom directory,
                // bypassing assistant selection entirely
                var plan = buildCustomDirPlan(contentManifest, sourceHandler,
                    contentTypeFilter, customDir, sourceHandler.getVersion());
                if (!dryRun) {
                    executeSetupPlan(plan, sourceHandler);
                }
                return toOutputDescriptors(plan);
            }

            var distribution = AiAssistExtensionsSourceHandler
                .readDistributionDescriptor(source == null);
            var selectedAssistants = selectAssistants(distribution, assistants, autoDetect);
            var planContext = new AiAssistExtensionsInstallPlanContext();
            var plan = buildSetupPlan(contentManifest, distribution, selectedAssistants,
                sourceHandler, planContext, contentTypeFilter,
                sourceHandler.getVersion());

            warnDuplicateContentDirs(distribution, selectedAssistants, contentTypeFilter);

            if (!dryRun) {
                executeSetupPlan(plan, sourceHandler);
                saveInstallationsState(selectedAssistants, distribution, plan);
            }
            return toOutputDescriptors(plan);
        }
    }

    // ──────────────────────────── Uninstall ────────────────────────────

    /**
     * Uninstall extensions from target directories. When {@code customDir} is
     * specified, only that directory is scanned. Otherwise, scans the union of
     * dirs from the distribution descriptor and fcli state.
     *
     * @param contentTypeFilter optional content type filter, or null for all
     * @param customDir         specific directory to uninstall from, or null for all known dirs
     * @param dryRun            if true, report only without deleting
     */
    public static List<AiAssistExtensionsOutputDescriptor> uninstall(
            Set<String> contentTypeFilter, String customDir, boolean dryRun) {
        var targetDirs = customDir != null
            ? Set.of(Path.of(customDir).toAbsolutePath().normalize())
            : collectAllKnownTargetDirs();
        var results = new ArrayList<AiAssistExtensionsOutputDescriptor>();

        for (var dir : targetDirs) {
            for (var manifest : readAllTargetDirManifests(dir)) {
                if (!matchesContentTypeFilter(manifest.getContentType(), contentTypeFilter)) {
                    continue;
                }

                var files = manifest.getFiles() != null ? manifest.getFiles() : List.<String>of();
                if (!dryRun) {
                    for (var file : files) {
                        deleteTargetFile(dir.resolve(file));
                    }
                    deleteManifestFile(dir, manifest.getContentType());
                }
                results.add(AiAssistExtensionsOutputDescriptor.builder()
                    .contentType(manifest.getContentType())
                    .targetDir(dir.toString())
                    .fileCount(files.size())
                    .sourceVersion(manifest.getVersion())
                    .files(files.toArray(String[]::new))
                    .filesString(String.join(", ", files))
                    .actionResult("REMOVED")
                    .build());
            }
        }

        if (!dryRun && customDir == null) {
            clearInstallationsState(contentTypeFilter);
        }
        return results;
    }

    // ──────────────────────────── List installed ────────────────────────────

    public static List<AiAssistExtensionsOutputDescriptor> listInstalled() {
        var installations = loadInstallationsState();
        var results = new ArrayList<AiAssistExtensionsOutputDescriptor>();

        for (var entry : installations.getAssistants().entrySet()) {
            var assistantId = entry.getKey();
            var installation = entry.getValue();
            for (var targetEntry : installation.getTargets().entrySet()) {
                var contentType = targetEntry.getKey();
                var targetDir = Path.of(targetEntry.getValue());
                var manifest = readTargetDirManifest(targetDir, contentType);
                var files = manifest != null && manifest.getFiles() != null
                    ? manifest.getFiles() : List.<String>of();
                var version = manifest != null ? manifest.getVersion() : null;
                results.add(AiAssistExtensionsOutputDescriptor.builder()
                    .assistant(installation.getDisplayName())
                    .assistantId(assistantId)
                    .contentType(contentType)
                    .targetDir(targetDir.toString())
                    .fileCount(files.size())
                    .sourceVersion(version)
                    .files(files.toArray(String[]::new))
                    .filesString(String.join(", ", files))
                    .build());
            }
        }
        return results;
    }

    // ──────────────────────────── List versions ────────────────────────────

    public static List<AiAssistExtensionsVersionOutputDescriptor> listVersions() {
        var defs = getToolDefinitions();
        var result = new ArrayList<AiAssistExtensionsVersionOutputDescriptor>();
        for (var v : defs.getVersions()) {
            result.add(AiAssistExtensionsVersionOutputDescriptor.builder()
                .version(v.getVersion())
                .aliases(v.getAliases() != null ? String.join(", ", v.getAliases()) : "")
                .stable(v.isStable())
                .build());
        }
        return result;
    }

    // ──────────────────────────── List assistants ────────────────────────────

    public static List<AiAssistExtensionsAssistantOutputDescriptor> listAssistants(boolean detect) {
        var distribution = AiAssistExtensionsSourceHandler.readDistributionDescriptor(true);
        if (distribution.getAssistants() == null) { return Collections.emptyList(); }

        var conditionEvaluator = detect ? new AiAssistExtensionsConditionEvaluator() : null;
        var installations = loadInstallationsState();

        var result = new ArrayList<AiAssistExtensionsAssistantOutputDescriptor>();
        for (var entry : distribution.getAssistants().entrySet()) {
            var id = entry.getKey();
            var assistant = entry.getValue();
            var contentTypes = assistant.getTargets() != null
                ? assistant.getTargets().stream()
                    .map(AiAssistExtensionsTargetDescriptor::getContentType)
                    .toArray(String[]::new)
                : new String[0];

            String detected = conditionEvaluator != null
                ? String.valueOf(conditionEvaluator.evaluate(assistant.getIfCondition()))
                : "N/A";

            var assistantInstallation = installations.getAssistants().get(id);
            var installed = assistantInstallation != null;
            String installedVersion = null;
            if (installed) {
                // Read version from first target dir manifest
                installedVersion = assistantInstallation.getTargets().entrySet().stream()
                    .map(e -> readTargetDirManifest(Path.of(e.getValue()), e.getKey()))
                    .filter(m -> m != null)
                    .map(AiAssistExtensionsTargetDirManifest::getVersion)
                    .findFirst().orElse(null);
            }

            result.add(AiAssistExtensionsAssistantOutputDescriptor.builder()
                .id(id)
                .name(assistant.getDisplayName())
                .contentTypes(contentTypes)
                .contentTypesString(String.join(", ", contentTypes))
                .detected(detected)
                .installed(installed)
                .installedVersion(installedVersion)
                .build());
        }
        return result;
    }

    // ──────────────────────────── Source resolution ────────────────────────────

    private static AiAssistExtensionsSourceHandler resolveSource(
            String source, String version, DigestMismatchAction onDigestMismatch) {
        if (source != null) {
            return AiAssistExtensionsSourceHandler.fromLocalSource(source);
        }
        var versionDesc = resolveVersion(version);
        return AiAssistExtensionsSourceHandler.fromToolDefinitions(versionDesc, onDigestMismatch);
    }

    // ──────────────────────────── Assistant selection ────────────────────────────

    private static Map<String, AiAssistExtensionsAssistantDescriptor> selectAssistants(
            AiAssistExtensionsDistributionDescriptor distribution,
            Set<String> explicitAssistants, boolean autoDetect) {
        var result = new LinkedHashMap<String, AiAssistExtensionsAssistantDescriptor>();
        if (distribution.getAssistants() == null) { return result; }

        if (explicitAssistants != null && !explicitAssistants.isEmpty()) {
            for (var id : explicitAssistants) {
                var assistant = distribution.getAssistants().get(id);
                if (assistant == null) {
                    throw new FcliSimpleException(
                        "Unknown assistant: " + id + ". Available: "
                        + String.join(", ", distribution.getAssistants().keySet()));
                }
                result.put(id, assistant);
            }
        } else if (autoDetect) {
            var evaluator = new AiAssistExtensionsConditionEvaluator();
            for (var entry : distribution.getAssistants().entrySet()) {
                if (evaluator.evaluate(entry.getValue().getIfCondition())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    // ──────────────────────────── Internal plan entry ────────────────────────────

    private record PlanEntry(
        String assistant, String assistantId, String contentType,
        String targetDir, String sourceFile, String targetRelPath,
        String targetAbsPath, String sourceVersion, String action) {}

    // ──────────────────────────── Setup plan ────────────────────────────

    /**
     * Build a setup plan that is idempotent: installs new files, updates changed
     * files, removes obsolete files, and reports unchanged files.
     */
    private static List<PlanEntry> buildSetupPlan(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsDistributionDescriptor distribution,
            Map<String, AiAssistExtensionsAssistantDescriptor> assistants,
            AiAssistExtensionsSourceHandler sourceHandler,
            AiAssistExtensionsInstallPlanContext planContext,
            Set<String> contentTypeFilter,
            String sourceVersion) {
        var plan = new ArrayList<PlanEntry>();

        for (var entry : assistants.entrySet()) {
            var assistantId = entry.getKey();
            var assistant = entry.getValue();
            if (assistant.getTargets() == null) { continue; }

            for (var target : assistant.getTargets()) {
                var contentType = target.getContentType();
                if (!matchesContentTypeFilter(contentType, contentTypeFilter)) { continue; }

                var resolvedDirs = AiAssistExtensionsPathResolver.resolveAll(target.getTargetDirs());
                if (resolvedDirs.isEmpty()) { continue; }

                var coveredDir = planContext.findCoveredDir(resolvedDirs, contentType);
                boolean isExisting = coveredDir != null;
                var resolvedDir = isExisting ? coveredDir : resolvedDirs.get(0);

                if (!isExisting) {
                    planContext.markCovered(resolvedDir, contentType);
                }

                if (isExisting) {
                    // Another assistant already handles this dir in this run
                    addExistingEntries(plan, assistant, assistantId, contentType,
                        resolvedDir, contentManifest, target, sourceHandler, sourceVersion);
                    continue;
                }

                // Read existing manifest from target dir for diff
                var existingManifest = readTargetDirManifest(resolvedDir, contentType);
                var existingFiles = existingManifest != null && existingManifest.getFiles() != null
                    ? new HashSet<>(existingManifest.getFiles()) : Set.<String>of();

                var sourceFiles = discoverSourceFiles(contentManifest, target, sourceHandler);
                var handledRelPaths = new HashSet<String>();
                for (var sourceFile : sourceFiles) {
                    var targetRelPath = getTargetRelativePath(contentManifest, target, sourceFile);
                    var targetAbsPath = resolvedDir.resolve(targetRelPath).toString();
                    handledRelPaths.add(targetRelPath);

                    String action;
                    if (!existingFiles.contains(targetRelPath)) {
                        action = "INSTALLED";
                    } else if (hasFileChanged(sourceHandler, sourceFile, Path.of(targetAbsPath))) {
                        action = "UPDATED";
                    } else {
                        action = "UNCHANGED";
                    }
                    plan.add(new PlanEntry(
                        assistant.getDisplayName(), assistantId, contentType,
                        resolvedDir.toString(), sourceFile, targetRelPath,
                        targetAbsPath, sourceVersion, action));
                }

                // Files in existing manifest but not in source → REMOVED
                for (var existingFile : existingFiles) {
                    if (!handledRelPaths.contains(existingFile)) {
                        plan.add(new PlanEntry(
                            assistant.getDisplayName(), assistantId, contentType,
                            resolvedDir.toString(), null, existingFile,
                            resolvedDir.resolve(existingFile).toString(),
                            sourceVersion, "REMOVED"));
                    }
                }
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
        var sourceFiles = discoverSourceFiles(contentManifest, target, sourceHandler);
        for (var sourceFile : sourceFiles) {
            var targetRelPath = getTargetRelativePath(contentManifest, target, sourceFile);
            plan.add(new PlanEntry(
                assistant.getDisplayName(), assistantId, contentType,
                resolvedDir.toString(), sourceFile, targetRelPath,
                resolvedDir.resolve(targetRelPath).toString(),
                sourceVersion, "EXISTING"));
        }
    }

    // ──────────────────────────── Custom-dir plan ────────────────────────────

    /**
     * Build a setup plan for --dir mode: installs content types directly to
     * a custom directory, bypassing assistant selection. Content is discovered
     * from the content manifest without relying on assistant-specific config.
     */
    private static List<PlanEntry> buildCustomDirPlan(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsSourceHandler sourceHandler,
            Set<String> contentTypeFilter, String customDir,
            String sourceVersion) {
        var plan = new ArrayList<PlanEntry>();
        var resolvedDir = Path.of(customDir).toAbsolutePath().normalize();
        if (contentManifest.getContentTypes() == null) { return plan; }

        for (var ctEntry : contentManifest.getContentTypes().entrySet()) {
            var contentType = ctEntry.getKey();
            if (!matchesContentTypeFilter(contentType, contentTypeFilter)) { continue; }

            var ctDesc = ctEntry.getValue();
            var existingManifest = readTargetDirManifest(resolvedDir, contentType);
            var existingFiles = existingManifest != null && existingManifest.getFiles() != null
                ? new HashSet<>(existingManifest.getFiles()) : Set.<String>of();

            var sourceFiles = discoverSourceFilesForContentType(ctDesc, sourceHandler);
            var handledRelPaths = new HashSet<String>();
            for (var sourceFile : sourceFiles) {
                var targetRelPath = getTargetRelativePathForContentType(ctDesc, sourceFile);
                var targetAbsPath = resolvedDir.resolve(targetRelPath).toString();
                handledRelPaths.add(targetRelPath);

                String action;
                if (!existingFiles.contains(targetRelPath)) {
                    action = "INSTALLED";
                } else if (hasFileChanged(sourceHandler, sourceFile, Path.of(targetAbsPath))) {
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
                        resolvedDir.resolve(existingFile).toString(),
                        sourceVersion, "REMOVED"));
                }
            }
        }
        return plan;
    }

    // ──────────────────────────── Plan → grouped output ────────────────────────────

    private static List<AiAssistExtensionsOutputDescriptor> toOutputDescriptors(List<PlanEntry> plan) {
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

    // ──────────────────────────── Content discovery ────────────────────────────

    private static List<String> discoverSourceFiles(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsTargetDescriptor target,
            AiAssistExtensionsSourceHandler sourceHandler) {
        var contentType = target.getContentType();
        var ctDesc = contentManifest.getContentTypes() != null
            ? contentManifest.getContentTypes().get(contentType) : null;
        if (ctDesc == null) { return Collections.emptyList(); }

        var discoverMode = ctDesc.getDiscover();
        if ("explicit".equals(discoverMode)) {
            return discoverExplicitEntries(ctDesc, target, sourceHandler);
        }

        var sourceDir = ctDesc.getSourceDir();
        if (sourceDir == null || !sourceHandler.exists(sourceDir)) {
            return Collections.emptyList();
        }

        if ("directory".equals(discoverMode)) {
            return discoverDirectoryEntries(sourceDir, ctDesc.getEntryMarker(), sourceHandler);
        } else if ("files".equals(discoverMode)) {
            return discoverFileEntries(sourceDir, ctDesc.getFilePattern(), sourceHandler);
        }
        return Collections.emptyList();
    }

    private static List<String> discoverDirectoryEntries(
            String sourceDir, String entryMarker,
            AiAssistExtensionsSourceHandler sourceHandler) {
        var result = new ArrayList<String>();
        sourceHandler.listDirs(sourceDir).forEach(dir -> {
            if (entryMarker != null) {
                var markerPath = dir.resolve(entryMarker);
                if (!sourceHandler.exists(markerPath.toString())) { return; }
            }
            sourceHandler.listFiles(dir.toString()).forEach(f -> {
                var relative = sourceHandler.getExtractedDir().relativize(
                    sourceHandler.getExtractedDir().resolve(f));
                result.add(relative.toString());
            });
        });
        return result;
    }

    private static List<String> discoverFileEntries(
            String sourceDir, String filePattern,
            AiAssistExtensionsSourceHandler sourceHandler) {
        var result = new ArrayList<String>();
        var globPattern = filePattern != null ? filePattern : "*";
        sourceHandler.listFiles(sourceDir).forEach(f -> {
            if (matchesGlob(f.getFileName().toString(), globPattern)) {
                result.add(f.toString());
            }
        });
        return result;
    }

    private static List<String> discoverExplicitEntries(
            AiAssistExtensionsContentTypeDescriptor ctDesc,
            AiAssistExtensionsTargetDescriptor target,
            AiAssistExtensionsSourceHandler sourceHandler) {
        if (target.getSourceEntries() == null) { return Collections.emptyList(); }
        var entriesMap = ctDesc.getEntries();
        var result = new ArrayList<String>();
        for (var entryName : target.getSourceEntries()) {
            var entryPath = entriesMap != null ? entriesMap.get(entryName) : entryName;
            if (entryPath == null) { entryPath = entryName; }
            if (sourceHandler.exists(entryPath)) {
                var resolvedPath = sourceHandler.getExtractedDir().resolve(entryPath);
                if (Files.isDirectory(resolvedPath)) {
                    sourceHandler.listFiles(entryPath).forEach(f -> result.add(f.toString()));
                } else {
                    result.add(entryPath);
                }
            }
        }
        return result;
    }

    private static String getTargetRelativePath(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsTargetDescriptor target, String sourceFile) {
        var contentType = target.getContentType();
        var ctDesc = contentManifest.getContentTypes() != null
            ? contentManifest.getContentTypes().get(contentType) : null;

        if (ctDesc == null) { return Path.of(sourceFile).getFileName().toString(); }

        var discoverMode = ctDesc.getDiscover();
        if ("explicit".equals(discoverMode)) {
            var entriesMap = ctDesc.getEntries();
            if (entriesMap != null && target.getSourceEntries() != null) {
                for (var entryName : target.getSourceEntries()) {
                    var entryPath = entriesMap.getOrDefault(entryName, entryName);
                    if (sourceFile.startsWith(entryPath + "/")) {
                        return sourceFile.substring(entryPath.length() + 1);
                    } else if (sourceFile.equals(entryPath)) {
                        return Path.of(sourceFile).getFileName().toString();
                    }
                }
            }
            return Path.of(sourceFile).getFileName().toString();
        }

        if (ctDesc.getSourceDir() != null && sourceFile.startsWith(ctDesc.getSourceDir() + "/")) {
            return sourceFile.substring(ctDesc.getSourceDir().length() + 1);
        }
        return sourceFile;
    }

    // ──────────────────────────── Custom-dir content discovery ────────────────────────────

    /**
     * Discover source files for a content type without assistant-specific target config.
     * For directory/files modes, behaves identically to the target-aware variant.
     * For explicit mode, discovers all entries defined in the content type.
     */
    private static List<String> discoverSourceFilesForContentType(
            AiAssistExtensionsContentTypeDescriptor ctDesc,
            AiAssistExtensionsSourceHandler sourceHandler) {
        var discoverMode = ctDesc.getDiscover();

        if ("explicit".equals(discoverMode)) {
            return discoverAllExplicitEntries(ctDesc, sourceHandler);
        }

        var sourceDir = ctDesc.getSourceDir();
        if (sourceDir == null || !sourceHandler.exists(sourceDir)) {
            return Collections.emptyList();
        }
        if ("directory".equals(discoverMode)) {
            return discoverDirectoryEntries(sourceDir, ctDesc.getEntryMarker(), sourceHandler);
        } else if ("files".equals(discoverMode)) {
            return discoverFileEntries(sourceDir, ctDesc.getFilePattern(), sourceHandler);
        }
        return Collections.emptyList();
    }

    /**
     * Discover all explicit entries defined in the content type descriptor,
     * without filtering by assistant-specific source-entries.
     */
    private static List<String> discoverAllExplicitEntries(
            AiAssistExtensionsContentTypeDescriptor ctDesc,
            AiAssistExtensionsSourceHandler sourceHandler) {
        var entriesMap = ctDesc.getEntries();
        if (entriesMap == null) { return Collections.emptyList(); }
        var result = new ArrayList<String>();
        for (var entryPath : entriesMap.values()) {
            if (entryPath != null && sourceHandler.exists(entryPath)) {
                var resolvedPath = sourceHandler.getExtractedDir().resolve(entryPath);
                if (Files.isDirectory(resolvedPath)) {
                    sourceHandler.listFiles(entryPath).forEach(f -> result.add(f.toString()));
                } else {
                    result.add(entryPath);
                }
            }
        }
        return result;
    }

    /**
     * Compute target-relative path for a source file using only the content type
     * descriptor (no assistant target). For explicit mode, strips entry path prefix.
     */
    private static String getTargetRelativePathForContentType(
            AiAssistExtensionsContentTypeDescriptor ctDesc, String sourceFile) {
        var discoverMode = ctDesc.getDiscover();
        if ("explicit".equals(discoverMode)) {
            var entriesMap = ctDesc.getEntries();
            if (entriesMap != null) {
                for (var entryPath : entriesMap.values()) {
                    if (entryPath != null && sourceFile.startsWith(entryPath + "/")) {
                        return sourceFile.substring(entryPath.length() + 1);
                    } else if (sourceFile.equals(entryPath)) {
                        return Path.of(sourceFile).getFileName().toString();
                    }
                }
            }
            return Path.of(sourceFile).getFileName().toString();
        }
        if (ctDesc.getSourceDir() != null && sourceFile.startsWith(ctDesc.getSourceDir() + "/")) {
            return sourceFile.substring(ctDesc.getSourceDir().length() + 1);
        }
        return sourceFile;
    }

    private static boolean matchesGlob(String filename, String glob) {
        var regex = glob.replace(".", "\\.").replace("*", ".*");
        return filename.matches(regex);
    }

    // ──────────────────────────── Plan execution ────────────────────────────

    private static void executeSetupPlan(List<PlanEntry> plan,
            AiAssistExtensionsSourceHandler sourceHandler) {
        // Group by (target dir, content type) to write one manifest per combo
        var byDirAndType = plan.stream()
            .filter(e -> !"EXISTING".equals(e.action()))
            .collect(Collectors.groupingBy(
                e -> e.targetDir() + "\0" + e.contentType(),
                LinkedHashMap::new, Collectors.toList()));

        for (var dirEntries : byDirAndType.values()) {
            for (var entry : dirEntries) {
                switch (entry.action()) {
                    case "INSTALLED", "UPDATED" -> installFile(sourceHandler, entry);
                    case "REMOVED" -> deleteTargetFile(Path.of(entry.targetAbsPath()));
                }
            }

            // Write manifest for this (target dir, content type) pair
            var first = dirEntries.get(0);
            var installedFiles = dirEntries.stream()
                .filter(e -> !"REMOVED".equals(e.action()))
                .map(PlanEntry::targetRelPath)
                .toList();
            writeTargetDirManifest(Path.of(first.targetDir()), first.contentType(),
                first.sourceVersion(), installedFiles);
        }
    }

    private static void installFile(AiAssistExtensionsSourceHandler sourceHandler, PlanEntry entry) {
        var targetPath = Path.of(entry.targetAbsPath());
        var sourceBytes = sourceHandler.readFileBytes(entry.sourceFile());
        if (sourceBytes == null) {
            throw new FcliSimpleException("Source file not found: " + entry.sourceFile());
        }
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, sourceBytes);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error installing file: " + targetPath, e);
        }
    }

    // ──────────────────────────── Target dir manifest ────────────────────────────

    private static void writeTargetDirManifest(Path targetDir, String contentType,
            String version, List<String> files) {
        var manifest = AiAssistExtensionsTargetDirManifest.builder()
            .schemaVersion(1)
            .contentType(contentType)
            .version(version)
            .timestamp(Instant.now().toString())
            .files(files)
            .build();
        var manifestPath = targetDir.resolve(
            AiAssistExtensionsTargetDirManifest.manifestFilename(contentType));
        try {
            Files.createDirectories(targetDir);
            var json = JsonHelper.getObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(manifest);
            Files.writeString(manifestPath, json);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error writing manifest: " + manifestPath, e);
        }
    }

    static AiAssistExtensionsTargetDirManifest readTargetDirManifest(
            Path targetDir, String contentType) {
        var manifestPath = targetDir.resolve(
            AiAssistExtensionsTargetDirManifest.manifestFilename(contentType));
        if (!Files.isRegularFile(manifestPath)) { return null; }
        return readManifestFile(manifestPath);
    }

    static List<AiAssistExtensionsTargetDirManifest> readAllTargetDirManifests(Path targetDir) {
        if (!Files.isDirectory(targetDir)) { return List.of(); }
        var glob = AiAssistExtensionsTargetDirManifest.manifestGlob();
        var result = new ArrayList<AiAssistExtensionsTargetDirManifest>();
        try (var stream = Files.newDirectoryStream(targetDir, glob)) {
            for (var path : stream) {
                var manifest = readManifestFile(path);
                if (manifest != null) { result.add(manifest); }
            }
        } catch (IOException e) {
            LOG.warn("Error listing manifests in: {}", targetDir, e);
        }
        return result;
    }

    private static AiAssistExtensionsTargetDirManifest readManifestFile(Path manifestPath) {
        try {
            var content = Files.readString(manifestPath);
            return JsonHelper.getObjectMapper()
                .readValue(content, AiAssistExtensionsTargetDirManifest.class);
        } catch (IOException e) {
            LOG.warn("Error reading manifest: {}", manifestPath, e);
            return null;
        }
    }

    private static void deleteManifestFile(Path targetDir, String contentType) {
        var manifestPath = targetDir.resolve(
            AiAssistExtensionsTargetDirManifest.manifestFilename(contentType));
        try {
            Files.deleteIfExists(manifestPath);
        } catch (IOException e) {
            LOG.warn("Error deleting manifest: {}", manifestPath, e);
        }
    }

    // ──────────────────────────── Installations state (fcli state) ────────────────────────────

    private static AiAssistExtensionsInstallationsDescriptor loadInstallationsState() {
        var desc = FcliDataHelper.readFile(INSTALLATIONS_STATE_PATH,
            AiAssistExtensionsInstallationsDescriptor.class, false);
        return desc != null ? desc : new AiAssistExtensionsInstallationsDescriptor();
    }

    private static void saveInstallationsState(
            Map<String, AiAssistExtensionsAssistantDescriptor> selectedAssistants,
            AiAssistExtensionsDistributionDescriptor distribution,
            List<PlanEntry> plan) {
        var existing = loadInstallationsState();

        // Build target dir map from plan for each assistant
        var planTargets = plan.stream()
            .filter(e -> !"EXISTING".equals(e.action()) && !"REMOVED".equals(e.action()))
            .collect(Collectors.groupingBy(PlanEntry::assistantId,
                LinkedHashMap::new, Collectors.toList()));

        for (var entry : selectedAssistants.entrySet()) {
            var assistantId = entry.getKey();
            var assistant = entry.getValue();
            var entries = planTargets.getOrDefault(assistantId, List.of());

            var targets = new LinkedHashMap<String, String>();
            for (var planEntry : entries) {
                targets.putIfAbsent(planEntry.contentType(), planEntry.targetDir());
            }
            // Also include targets from EXISTING entries (shared dirs)
            plan.stream()
                .filter(e -> "EXISTING".equals(e.action()) && e.assistantId().equals(assistantId))
                .forEach(e -> targets.putIfAbsent(e.contentType(), e.targetDir()));

            if (!targets.isEmpty()) {
                existing.getAssistants().put(assistantId, AssistantInstallation.builder()
                    .displayName(assistant.getDisplayName())
                    .targets(targets)
                    .build());
            }
        }

        FcliDataHelper.saveFile(INSTALLATIONS_STATE_PATH, existing, true);
    }

    private static void clearInstallationsState(Set<String> contentTypeFilter) {
        if (contentTypeFilter == null || contentTypeFilter.isEmpty()) {
            FcliDataHelper.deleteFile(INSTALLATIONS_STATE_PATH, false);
            return;
        }
        // Partial clear: remove only matching content types from each assistant
        var state = loadInstallationsState();
        var toRemove = new ArrayList<String>();
        for (var entry : state.getAssistants().entrySet()) {
            entry.getValue().getTargets().keySet().removeAll(contentTypeFilter);
            if (entry.getValue().getTargets().isEmpty()) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(state.getAssistants()::remove);
        if (state.getAssistants().isEmpty()) {
            FcliDataHelper.deleteFile(INSTALLATIONS_STATE_PATH, false);
        } else {
            FcliDataHelper.saveFile(INSTALLATIONS_STATE_PATH, state, true);
        }
    }

    // ──────────────────────────── Target dir collection ────────────────────────────

    /**
     * Collect all known target directories from both the distribution descriptor
     * (if available) and the fcli installations state.
     */
    private static Set<Path> collectAllKnownTargetDirs() {
        var dirs = new LinkedHashSet<Path>();

        // From distribution descriptor (if tool-definitions available)
        try {
            var distribution = AiAssistExtensionsSourceHandler.readDistributionDescriptor(true);
            if (distribution.getAssistants() != null) {
                for (var assistant : distribution.getAssistants().values()) {
                    if (assistant.getTargets() == null) { continue; }
                    for (var target : assistant.getTargets()) {
                        dirs.addAll(AiAssistExtensionsPathResolver.resolveAll(target.getTargetDirs()));
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Distribution descriptor not available for uninstall scan", e);
        }

        // From fcli state
        var installations = loadInstallationsState();
        for (var installation : installations.getAssistants().values()) {
            for (var dir : installation.getTargets().values()) {
                dirs.add(Path.of(dir));
            }
        }

        return dirs;
    }

    // ──────────────────────────── Duplicate content warning ────────────────────────────

    /**
     * Warn when a content type is present in multiple directories that an
     * assistant reads from, which may cause duplicate entries in that assistant.
     */
    private static void warnDuplicateContentDirs(
            AiAssistExtensionsDistributionDescriptor distribution,
            Map<String, AiAssistExtensionsAssistantDescriptor> selectedAssistants,
            Set<String> contentTypeFilter) {
        if (distribution.getAssistants() == null) { return; }

        for (var entry : distribution.getAssistants().entrySet()) {
            var assistantId = entry.getKey();
            var assistant = entry.getValue();
            if (assistant.getTargets() == null) { continue; }

            for (var target : assistant.getTargets()) {
                var contentType = target.getContentType();
                if (!matchesContentTypeFilter(contentType, contentTypeFilter)) { continue; }

                var resolvedDirs = AiAssistExtensionsPathResolver.resolveAll(target.getTargetDirs());
                var dirsWithManifest = resolvedDirs.stream()
                    .filter(dir -> readTargetDirManifest(dir, contentType) != null
                        || selectedAssistants.containsKey(assistantId))
                    .filter(dir -> readTargetDirManifest(dir, contentType) != null)
                    .toList();

                if (dirsWithManifest.size() > 1) {
                    LOG.warn("Content type '{}' exists in multiple directories accessible by {}: {}. "
                        + "This may cause duplicate entries in the assistant. Consider running "
                        + "'uninstall' to clean up before re-running 'setup'.",
                        contentType, assistant.getDisplayName(),
                        dirsWithManifest.stream().map(Path::toString)
                            .collect(Collectors.joining(", ")));
                }
            }
        }
    }

    // ──────────────────────────── File operations ────────────────────────────

    private static void deleteTargetFile(Path targetPath) {
        try {
            Files.deleteIfExists(targetPath);
            var parent = targetPath.getParent();
            while (parent != null && Files.isDirectory(parent)) {
                try (var stream = Files.list(parent)) {
                    if (stream.findAny().isEmpty()) {
                        Files.delete(parent);
                        parent = parent.getParent();
                    } else {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("Error deleting file: {}", targetPath, e);
        }
    }

    private static boolean hasFileChanged(
            AiAssistExtensionsSourceHandler sourceHandler, String sourceFile, Path targetPath) {
        if (!Files.isRegularFile(targetPath)) { return true; }
        try {
            var sourceBytes = sourceHandler.readFileBytes(sourceFile);
            var targetBytes = Files.readAllBytes(targetPath);
            return sourceBytes == null || !Arrays.equals(sourceBytes, targetBytes);
        } catch (IOException e) {
            return true;
        }
    }

    private static boolean matchesContentTypeFilter(String contentType, Set<String> filter) {
        return filter == null || filter.isEmpty() || filter.contains(contentType);
    }
}
