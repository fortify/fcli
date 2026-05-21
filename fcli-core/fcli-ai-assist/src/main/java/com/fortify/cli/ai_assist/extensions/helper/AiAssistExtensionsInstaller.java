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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsSourceHandler.DigestMismatchAction;
import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsStateDescriptor.FileEntry;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

/**
 * Core install/update/uninstall/list logic for AI assistant extensions.
 * Output is grouped by (assistant, contentType, targetDir).
 */
public final class AiAssistExtensionsInstaller {
    private static final Logger LOG = LoggerFactory.getLogger(AiAssistExtensionsInstaller.class);
    private static final Path STATE_BASE_PATH = Path.of("state", "ai-assist", "extensions");

    private AiAssistExtensionsInstaller() {}

    // ──────────────────────────── Version resolution ────────────────────────────

    public static ToolDefinitionRootDescriptor getToolDefinitions() {
        return ToolDefinitionsHelper.getToolDefinitionRootDescriptor(
            AiAssistExtensionsSourceHandler.TOOL_NAME);
    }

    public static ToolDefinitionVersionDescriptor resolveVersion(String version) {
        return getToolDefinitions().getVersionOrDefault(version);
    }

    // ──────────────────────────── Install ────────────────────────────

    public static List<AiAssistExtensionsOutputDescriptor> install(
            String source, String version,
            Set<String> assistantFilter, Set<String> excludeAssistants,
            Set<String> contentTypeFilter, String customDir,
            DigestMismatchAction onDigestMismatch, boolean dryRun) {

        try (var sourceHandler = resolveSource(source, version, onDigestMismatch)) {
            var contentManifest = sourceHandler.readContentManifest();
            var distribution = AiAssistExtensionsSourceHandler
                .readDistributionDescriptor(source == null);
            var conditionEvaluator = new AiAssistExtensionsConditionEvaluator();
            var planContext = new AiAssistExtensionsInstallPlanContext();

            var assistants = detectAssistants(distribution, conditionEvaluator,
                assistantFilter, excludeAssistants);
            var plan = buildInstallPlan(contentManifest, distribution, assistants,
                sourceHandler, planContext, contentTypeFilter, customDir,
                sourceHandler.getVersion());

            if (!dryRun) {
                executePlan(plan, sourceHandler);
            }
            return toOutputDescriptors(plan);
        }
    }

    // ──────────────────────────── Update ────────────────────────────

    public static List<AiAssistExtensionsOutputDescriptor> update(
            String source, String version,
            Set<String> assistantFilter, Set<String> excludeAssistants,
            Set<String> contentTypeFilter, String customDir,
            DigestMismatchAction onDigestMismatch, boolean dryRun) {

        try (var sourceHandler = resolveSource(source, version, onDigestMismatch)) {
            var contentManifest = sourceHandler.readContentManifest();
            var distribution = AiAssistExtensionsSourceHandler
                .readDistributionDescriptor(source == null);
            var conditionEvaluator = new AiAssistExtensionsConditionEvaluator();
            var planContext = new AiAssistExtensionsInstallPlanContext();

            var assistants = detectAssistants(distribution, conditionEvaluator,
                assistantFilter, excludeAssistants);
            var plan = buildUpdatePlan(contentManifest, distribution, assistants,
                sourceHandler, planContext, contentTypeFilter, customDir,
                sourceHandler.getVersion());

            if (!dryRun) {
                executeUpdatePlan(plan, sourceHandler);
            }
            return toOutputDescriptors(plan);
        }
    }

    // ──────────────────────────── Uninstall ────────────────────────────

    public static List<AiAssistExtensionsOutputDescriptor> uninstall(
            Set<String> assistantFilter, Set<String> excludeAssistants,
            boolean dryRun) {
        var stateEntries = loadAllStateDescriptors();
        var results = new ArrayList<AiAssistExtensionsOutputDescriptor>();

        for (var state : stateEntries) {
            if (!matchesFilter(state.getAssistantId(), assistantFilter, excludeAssistants)) {
                continue;
            }
            if (!dryRun) {
                for (var file : state.getFiles()) {
                    deleteTargetFile(Path.of(file.getTarget()));
                }
                deleteStateDescriptor(state.getAssistantId(), state.getContentType());
            }
            results.add(stateToOutput(state, "REMOVED"));
        }
        if (!dryRun) { cleanEmptyStateDirs(); }
        return results;
    }

    // ──────────────────────────── List installed ────────────────────────────

    public static List<AiAssistExtensionsOutputDescriptor> listInstalled() {
        return loadAllStateDescriptors().stream()
            .map(s -> stateToOutput(s, null))
            .toList();
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
        var installedState = loadAllStateDescriptors();
        var installedByAssistant = installedState.stream()
            .collect(Collectors.groupingBy(AiAssistExtensionsStateDescriptor::getAssistantId));

        var result = new ArrayList<AiAssistExtensionsAssistantOutputDescriptor>();
        for (var entry : distribution.getAssistants().entrySet()) {
            var id = entry.getKey();
            var assistant = entry.getValue();
            var contentTypes = assistant.getTargets() != null
                ? assistant.getTargets().stream()
                    .map(AiAssistExtensionsTargetDescriptor::getContentType)
                    .toArray(String[]::new)
                : new String[0];

            String detected;
            if (conditionEvaluator != null) {
                detected = String.valueOf(conditionEvaluator.evaluate(assistant.getIfCondition()));
            } else {
                detected = "N/A";
            }

            var assistantStates = installedByAssistant.getOrDefault(id, Collections.emptyList());
            var installed = !assistantStates.isEmpty();
            var installedVersion = assistantStates.stream()
                .map(AiAssistExtensionsStateDescriptor::getSourceVersion)
                .findFirst().orElse(null);

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

    // ──────────────────────────── Assistant detection ────────────────────────────

    private static Map<String, AiAssistExtensionsAssistantDescriptor> detectAssistants(
            AiAssistExtensionsDistributionDescriptor distribution,
            AiAssistExtensionsConditionEvaluator evaluator,
            Set<String> assistantFilter, Set<String> excludeAssistants) {
        var result = new LinkedHashMap<String, AiAssistExtensionsAssistantDescriptor>();
        if (distribution.getAssistants() == null) { return result; }

        for (var entry : distribution.getAssistants().entrySet()) {
            var id = entry.getKey();
            var assistant = entry.getValue();
            if (!matchesFilter(id, assistantFilter, excludeAssistants)) { continue; }
            boolean explicitlySelected = assistantFilter != null && !assistantFilter.isEmpty();
            if (explicitlySelected || evaluator.evaluate(assistant.getIfCondition())) {
                result.put(id, assistant);
            }
        }
        return result;
    }

    private static boolean matchesFilter(String id, Set<String> include, Set<String> exclude) {
        if (include != null && !include.isEmpty() && !include.contains(id)) { return false; }
        if (exclude != null && exclude.contains(id)) { return false; }
        return true;
    }

    // ──────────────────────────── Internal plan entry ────────────────────────────

    /**
     * Internal per-file plan entry used during plan construction.
     * Aggregated into grouped output descriptors after planning.
     */
    private record PlanEntry(
        String assistant, String assistantId, String contentType,
        String targetDir, String sourceFile, String targetPath,
        String sourceVersion, String action) {}

    // ──────────────────────────── Install plan ────────────────────────────

    private static List<PlanEntry> buildInstallPlan(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsDistributionDescriptor distribution,
            Map<String, AiAssistExtensionsAssistantDescriptor> assistants,
            AiAssistExtensionsSourceHandler sourceHandler,
            AiAssistExtensionsInstallPlanContext planContext,
            Set<String> contentTypeFilter, String customDir,
            String sourceVersion) {
        var plan = new ArrayList<PlanEntry>();

        for (var entry : assistants.entrySet()) {
            var assistantId = entry.getKey();
            var assistant = entry.getValue();
            if (assistant.getTargets() == null) { continue; }

            for (var target : assistant.getTargets()) {
                var contentType = target.getContentType();
                if (!matchesContentTypeFilter(contentType, contentTypeFilter)) { continue; }

                var resolvedDirs = customDir != null
                    ? List.of(Path.of(customDir).toAbsolutePath().normalize())
                    : AiAssistExtensionsPathResolver.resolveAll(target.getTargetDirs());
                if (resolvedDirs.isEmpty()) { continue; }

                var coveredDir = planContext.findCoveredDir(resolvedDirs, contentType);
                boolean isExisting = coveredDir != null;
                var resolvedDir = isExisting ? coveredDir : resolvedDirs.get(0);

                if (!isExisting) {
                    planContext.markCovered(resolvedDir, contentType);
                }

                var sourceFiles = discoverSourceFiles(contentManifest, target, sourceHandler);
                for (var sourceFile : sourceFiles) {
                    var targetRelPath = getTargetRelativePath(contentManifest, target, sourceFile);
                    var targetPath = resolvedDir.resolve(targetRelPath).toString();
                    plan.add(new PlanEntry(
                        assistant.getDisplayName(), assistantId, contentType,
                        resolvedDir.toString(), sourceFile, targetPath,
                        sourceVersion, isExisting ? "EXISTING" : "INSTALLED"));
                }
            }
        }
        return plan;
    }

    // ──────────────────────────── Update plan ────────────────────────────

    private static List<PlanEntry> buildUpdatePlan(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsDistributionDescriptor distribution,
            Map<String, AiAssistExtensionsAssistantDescriptor> assistants,
            AiAssistExtensionsSourceHandler sourceHandler,
            AiAssistExtensionsInstallPlanContext planContext,
            Set<String> contentTypeFilter, String customDir,
            String sourceVersion) {
        var installPlan = buildInstallPlan(contentManifest, distribution, assistants,
            sourceHandler, planContext, contentTypeFilter, customDir, sourceVersion);

        var existingState = loadAllStateDescriptors();
        // Build lookup: "assistantId:contentType:sourceFile" → target path
        var existingByKey = new LinkedHashMap<String, String>();
        for (var state : existingState) {
            for (var file : state.getFiles()) {
                existingByKey.put(state.getAssistantId() + ":" + state.getContentType()
                    + ":" + file.getSource(), file.getTarget());
            }
        }

        var plan = new ArrayList<PlanEntry>();
        var handledKeys = new java.util.HashSet<String>();

        for (var entry : installPlan) {
            var key = entry.assistantId() + ":" + entry.contentType()
                + ":" + entry.sourceFile();
            handledKeys.add(key);

            if ("EXISTING".equals(entry.action())) {
                plan.add(entry);
                continue;
            }

            var existingTarget = existingByKey.get(key);
            if (existingTarget == null) {
                plan.add(new PlanEntry(entry.assistant(), entry.assistantId(),
                    entry.contentType(), entry.targetDir(), entry.sourceFile(),
                    entry.targetPath(), sourceVersion, "INSTALLED"));
            } else if (hasFileChanged(sourceHandler, entry.sourceFile(), Path.of(existingTarget))) {
                plan.add(new PlanEntry(entry.assistant(), entry.assistantId(),
                    entry.contentType(), entry.targetDir(), entry.sourceFile(),
                    entry.targetPath(), sourceVersion, "UPDATED"));
            } else {
                plan.add(new PlanEntry(entry.assistant(), entry.assistantId(),
                    entry.contentType(), entry.targetDir(), entry.sourceFile(),
                    entry.targetPath(), sourceVersion, "UNCHANGED"));
            }
        }

        // Files in state but not in source → REMOVED
        for (var state : existingState) {
            if (!matchesFilter(state.getAssistantId(),
                    assistants.isEmpty() ? null : assistants.keySet(), null)) {
                continue;
            }
            for (var file : state.getFiles()) {
                var key = state.getAssistantId() + ":" + state.getContentType()
                    + ":" + file.getSource();
                if (!handledKeys.contains(key)) {
                    plan.add(new PlanEntry(state.getAssistant(), state.getAssistantId(),
                        state.getContentType(), state.getTargetDir(), file.getSource(),
                        file.getTarget(), sourceVersion, "REMOVED"));
                }
            }
        }
        return plan;
    }

    // ──────────────────────────── Plan → grouped output ────────────────────────────

    /**
     * Aggregate per-file plan entries into grouped output descriptors,
     * one per (assistantId, contentType, targetDir, action).
     */
    private static List<AiAssistExtensionsOutputDescriptor> toOutputDescriptors(List<PlanEntry> plan) {
        // Group by (assistantId, contentType, targetDir, action)
        var groups = plan.stream().collect(Collectors.groupingBy(
            e -> e.assistantId() + "\0" + e.contentType() + "\0" + e.targetDir() + "\0" + e.action(),
            LinkedHashMap::new, Collectors.toList()));

        var result = new ArrayList<AiAssistExtensionsOutputDescriptor>();
        for (var entries : groups.values()) {
            var first = entries.get(0);
            var files = entries.stream()
                .map(e -> targetRelativePath(e.targetDir(), e.targetPath()))
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

    private static String targetRelativePath(String targetDir, String targetPath) {
        var dirPath = Path.of(targetDir);
        var fullPath = Path.of(targetPath);
        if (fullPath.startsWith(dirPath)) {
            return dirPath.relativize(fullPath).toString();
        }
        return targetPath;
    }

    // ──────────────────────────── State → output ────────────────────────────

    private static AiAssistExtensionsOutputDescriptor stateToOutput(
            AiAssistExtensionsStateDescriptor state, String action) {
        var files = state.getFiles() != null
            ? state.getFiles().stream()
                .map(f -> targetRelativePath(state.getTargetDir(), f.getTarget()))
                .toArray(String[]::new)
            : new String[0];
        return AiAssistExtensionsOutputDescriptor.builder()
            .assistant(state.getAssistant())
            .assistantId(state.getAssistantId())
            .contentType(state.getContentType())
            .targetDir(state.getTargetDir())
            .fileCount(files.length)
            .sourceVersion(state.getSourceVersion())
            .files(files)
            .filesString(String.join(", ", files))
            .actionResult(action)
            .build();
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

    private static boolean matchesGlob(String filename, String glob) {
        var regex = glob.replace(".", "\\.").replace("*", ".*");
        return filename.matches(regex);
    }

    // ──────────────────────────── Plan execution ────────────────────────────

    private static void executePlan(List<PlanEntry> plan,
            AiAssistExtensionsSourceHandler sourceHandler) {
        for (var entry : plan) {
            if ("INSTALLED".equals(entry.action())) {
                installFile(sourceHandler, entry);
            }
        }
        savePlanState(plan);
    }

    private static void executeUpdatePlan(List<PlanEntry> plan,
            AiAssistExtensionsSourceHandler sourceHandler) {
        for (var entry : plan) {
            switch (entry.action()) {
                case "INSTALLED", "UPDATED" -> installFile(sourceHandler, entry);
                case "REMOVED" -> deleteTargetFile(Path.of(entry.targetPath()));
            }
        }
        savePlanState(plan);
        cleanEmptyStateDirs();
    }

    private static void installFile(AiAssistExtensionsSourceHandler sourceHandler, PlanEntry entry) {
        var targetPath = Path.of(entry.targetPath());
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

    // ──────────────────────────── State management ────────────────────────────

    /**
     * Save grouped state descriptors from a plan.
     * Groups plan entries by (assistantId, contentType) and writes one state file each.
     * Entries with action REMOVED cause their state file to be deleted.
     */
    private static void savePlanState(List<PlanEntry> plan) {
        // Group by (assistantId, contentType)
        var groups = plan.stream().collect(Collectors.groupingBy(
            e -> e.assistantId() + "\0" + e.contentType(),
            LinkedHashMap::new, Collectors.toList()));

        for (var groupEntries : groups.values()) {
            var first = groupEntries.get(0);
            // Collect non-removed files
            var files = groupEntries.stream()
                .filter(e -> !"REMOVED".equals(e.action()) && !"EXISTING".equals(e.action()))
                .map(e -> FileEntry.builder()
                    .source(e.sourceFile())
                    .target(e.targetPath())
                    .build())
                .toList();

            if (files.isEmpty()) {
                // All removed or all existing — delete state
                deleteStateDescriptor(first.assistantId(), first.contentType());
            } else {
                var state = AiAssistExtensionsStateDescriptor.builder()
                    .assistant(first.assistant())
                    .assistantId(first.assistantId())
                    .contentType(first.contentType())
                    .targetDir(first.targetDir())
                    .sourceVersion(first.sourceVersion())
                    .timestamp(Instant.now().toString())
                    .files(files)
                    .build();
                var relativePath = STATE_BASE_PATH
                    .resolve(first.assistantId())
                    .resolve(first.contentType() + ".json");
                FcliDataHelper.saveFile(relativePath, state, true);
            }
        }
    }

    private static void deleteStateDescriptor(String assistantId, String contentType) {
        var relativePath = STATE_BASE_PATH
            .resolve(assistantId)
            .resolve(contentType + ".json");
        FcliDataHelper.deleteFile(relativePath, false);
    }

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

    static List<AiAssistExtensionsStateDescriptor> loadAllStateDescriptors() {
        var result = new ArrayList<AiAssistExtensionsStateDescriptor>();
        var basePath = FcliDataHelper.getFcliHomePath().resolve(STATE_BASE_PATH);
        if (!Files.isDirectory(basePath)) { return result; }

        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".json")) {
                        try {
                            var content = Files.readString(file);
                            var desc = JsonHelper.getObjectMapper()
                                .readValue(content, AiAssistExtensionsStateDescriptor.class);
                            result.add(desc);
                        } catch (IOException e) {
                            LOG.warn("Error reading state file: {}", file, e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.warn("Error walking state directory: {}", basePath, e);
        }
        return result;
    }

    private static void cleanEmptyStateDirs() {
        var basePath = FcliDataHelper.getFcliHomePath().resolve(STATE_BASE_PATH);
        if (!Files.isDirectory(basePath)) { return; }
        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (!dir.equals(basePath)) {
                        try (var stream = Files.list(dir)) {
                            if (stream.findAny().isEmpty()) { Files.delete(dir); }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.debug("Error cleaning empty state dirs", e);
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
