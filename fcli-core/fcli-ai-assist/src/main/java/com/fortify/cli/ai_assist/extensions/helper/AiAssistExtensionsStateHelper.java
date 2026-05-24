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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsInstallationsDescriptor.AssistantInstallation;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliDataHelper;

/**
 * Manages persistent state for AI assistant extensions:
 * <ul>
 *   <li>Target-dir manifests ({@code .fortify-extensions.<contentType>.json})</li>
 *   <li>Fcli installations state ({@code installations.json})</li>
 *   <li>File-level operations (install, delete, compare)</li>
 * </ul>
 */
final class AiAssistExtensionsStateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AiAssistExtensionsStateHelper.class);
    private static final Path INSTALLATIONS_STATE_PATH =
        Path.of("state", "ai-assist", "extensions", "installations.json");

    private AiAssistExtensionsStateHelper() {}

    // ──────────────────────────── Target dir manifest I/O ────────────────────────────

    static void writeTargetDirManifest(Path targetDir, String contentType,
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

    static void deleteManifestFile(Path targetDir, String contentType) {
        var manifestPath = targetDir.resolve(
            AiAssistExtensionsTargetDirManifest.manifestFilename(contentType));
        try {
            Files.deleteIfExists(manifestPath);
        } catch (IOException e) {
            LOG.warn("Error deleting manifest: {}", manifestPath, e);
        }
    }

    // ──────────────────────────── Installations state (fcli state) ────────────────────────────

    static AiAssistExtensionsInstallationsDescriptor loadInstallationsState() {
        var desc = FcliDataHelper.readFile(INSTALLATIONS_STATE_PATH,
            AiAssistExtensionsInstallationsDescriptor.class, false);
        return desc != null ? desc : new AiAssistExtensionsInstallationsDescriptor();
    }

    static void saveInstallationsState(
            Map<String, AiAssistExtensionsAssistantDescriptor> selectedAssistants,
            List<AiAssistExtensionsInstallPlanHelper.PlanEntry> plan) {
        var existing = loadInstallationsState();

        var planTargets = plan.stream()
            .filter(e -> !"EXISTING".equals(e.action()) && !"REMOVED".equals(e.action()))
            .collect(Collectors.groupingBy(
                AiAssistExtensionsInstallPlanHelper.PlanEntry::assistantId,
                LinkedHashMap::new, Collectors.toList()));

        for (var entry : selectedAssistants.entrySet()) {
            var assistantId = entry.getKey();
            var assistant = entry.getValue();
            var entries = planTargets.getOrDefault(assistantId, List.of());

            var targets = new LinkedHashMap<String, String>();
            for (var planEntry : entries) {
                targets.putIfAbsent(planEntry.contentType(), planEntry.targetDir());
            }
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

    static void clearInstallationsState(Set<String> contentTypeFilter) {
        if (contentTypeFilter == null || contentTypeFilter.isEmpty()) {
            FcliDataHelper.deleteFile(INSTALLATIONS_STATE_PATH, false);
            return;
        }
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

    static Set<Path> collectAllKnownTargetDirs() {
        var dirs = new LinkedHashSet<Path>();

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

        var installations = loadInstallationsState();
        for (var installation : installations.getAssistants().values()) {
            for (var dir : installation.getTargets().values()) {
                dirs.add(Path.of(dir));
            }
        }

        return dirs;
    }

    // ──────────────────────────── File operations ────────────────────────────

    static void installFile(AiAssistExtensionsSourceHandler sourceHandler,
            String sourceFile, Path targetPath) {
        var sourceBytes = sourceHandler.readFileBytes(sourceFile);
        if (sourceBytes == null) {
            throw new FcliSimpleException("Source file not found: " + sourceFile);
        }
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, sourceBytes);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error installing file: " + targetPath, e);
        }
    }

    static void deleteTargetFile(Path targetPath) {
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

    static boolean hasFileChanged(
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

    static Path safeResolve(Path baseDir, String relativePath) {
        var resolved = baseDir.resolve(relativePath).normalize();
        if (!resolved.startsWith(baseDir.normalize())) {
            throw new FcliSimpleException(
                "Path traversal detected: " + relativePath);
        }
        return resolved;
    }

    static boolean matchesContentTypeFilter(String contentType, Set<String> filter) {
        return filter == null || filter.isEmpty() || filter.contains(contentType);
    }
}
