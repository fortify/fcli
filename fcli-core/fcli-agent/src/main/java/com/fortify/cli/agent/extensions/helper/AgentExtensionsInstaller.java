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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliDataHelper;

/**
 * Core install/update/uninstall/status logic for agent extensions.
 */
public final class AgentExtensionsInstaller {
    private static final Logger LOG = LoggerFactory.getLogger(AgentExtensionsInstaller.class);
    private static final Path STATE_BASE_PATH = Path.of("state", "agent", "extensions");

    /** Signature/version policy action */
    public enum PolicyAction { ignore, warn, fail }

    private AgentExtensionsInstaller() {}

    // ──────────────────────────── Install ────────────────────────────

    public static List<AgentExtensionsOutputDescriptor> install(
            String source,
            Set<String> assistantFilter,
            Set<String> excludeAssistants,
            Set<String> contentTypeFilter,
            String customDir,
            PolicyAction onInvalidSignature,
            PolicyAction onUnsigned,
            PolicyAction onInvalidVersion,
            boolean dryRun) {
        try (var sourceHandler = AgentExtensionsSourceHandler.resolve(source)) {
            var descriptor = sourceHandler.readDescriptor();
            var sourceVersion = sourceHandler.readSourceVersion();

            validateSchemaVersion(descriptor.getSchemaVersion(), onInvalidVersion);
            verifyDescriptorSignature(sourceHandler, onInvalidSignature, onUnsigned);

            var planContext = new AgentExtensionsInstallPlanContext();
            var conditionEvaluator = new AgentExtensionsConditionEvaluator(planContext);

            var assistants = detectAssistants(descriptor, conditionEvaluator,
                assistantFilter, excludeAssistants);

            var plan = buildInstallPlan(descriptor, assistants, sourceHandler,
                conditionEvaluator, planContext, contentTypeFilter, customDir, sourceVersion);

            verifyPlanSignatures(plan, sourceHandler, onInvalidSignature, onUnsigned);

            if (!dryRun) {
                executePlan(plan, sourceHandler);
            }
            return plan;
        }
    }

    // ──────────────────────────── Update ────────────────────────────

    public static List<AgentExtensionsOutputDescriptor> update(
            String source,
            Set<String> assistantFilter,
            Set<String> excludeAssistants,
            Set<String> contentTypeFilter,
            String customDir,
            PolicyAction onInvalidSignature,
            PolicyAction onUnsigned,
            PolicyAction onInvalidVersion,
            boolean dryRun) {
        try (var sourceHandler = AgentExtensionsSourceHandler.resolve(source)) {
            var descriptor = sourceHandler.readDescriptor();
            var sourceVersion = sourceHandler.readSourceVersion();

            validateSchemaVersion(descriptor.getSchemaVersion(), onInvalidVersion);
            verifyDescriptorSignature(sourceHandler, onInvalidSignature, onUnsigned);

            var planContext = new AgentExtensionsInstallPlanContext();
            var conditionEvaluator = new AgentExtensionsConditionEvaluator(planContext);

            var assistants = detectAssistants(descriptor, conditionEvaluator,
                assistantFilter, excludeAssistants);

            var plan = buildUpdatePlan(descriptor, assistants, sourceHandler,
                conditionEvaluator, planContext, contentTypeFilter, customDir, sourceVersion);

            // Only verify signatures for files being installed or updated
            var toVerify = plan.stream()
                .filter(o -> "INSTALLED".equals(o.getActionResult()) || "UPDATED".equals(o.getActionResult()))
                .toList();
            verifyPlanSignatures(toVerify, sourceHandler, onInvalidSignature, onUnsigned);

            if (!dryRun) {
                executeUpdatePlan(plan, sourceHandler);
            }
            return plan;
        }
    }

    // ──────────────────────────── Uninstall ────────────────────────────

    public static List<AgentExtensionsOutputDescriptor> uninstall(
            Set<String> assistantFilter,
            Set<String> excludeAssistants,
            boolean dryRun) {
        var results = new ArrayList<AgentExtensionsOutputDescriptor>();
        var stateEntries = loadAllStateDescriptors();

        for (var entry : stateEntries) {
            var assistantId = entry.getAssistantId();
            if (!matchesFilter(assistantId, assistantFilter, excludeAssistants)) { continue; }

            if (!dryRun) {
                deleteTargetFile(Path.of(entry.getTargetPath()));
                deleteStateDescriptor(assistantId, entry.getFile());
            }
            results.add(AgentExtensionsOutputDescriptor.builder()
                .assistant(entry.getAssistant())
                .assistantId(assistantId)
                .file(entry.getFile())
                .contentType(entry.getContentType())
                .targetDir(entry.getTargetDir())
                .targetPath(entry.getTargetPath())
                .sourceVersion(entry.getSourceVersion())
                .actionResult("REMOVED")
                .build());
        }
        // Clean up empty assistant dirs
        if (!dryRun) {
            cleanEmptyStateDirs();
        }
        return results;
    }

    // ──────────────────────────── Status ────────────────────────────

    public static List<AgentExtensionsOutputDescriptor> status() {
        var stateEntries = loadAllStateDescriptors();
        return stateEntries.stream()
            .map(s -> AgentExtensionsOutputDescriptor.builder()
                .assistant(s.getAssistant())
                .assistantId(s.getAssistantId())
                .file(s.getFile())
                .contentType(s.getContentType())
                .targetDir(s.getTargetDir())
                .targetPath(s.getTargetPath())
                .sourceVersion(s.getSourceVersion())
                .build())
            .toList();
    }

    // ──────────────────────────── Assistant detection ────────────────────────────

    private static Map<String, AgentExtensionsAssistantDescriptor> detectAssistants(
            AgentExtensionsDistributionDescriptor descriptor,
            AgentExtensionsConditionEvaluator evaluator,
            Set<String> assistantFilter,
            Set<String> excludeAssistants) {
        var result = new LinkedHashMap<String, AgentExtensionsAssistantDescriptor>();
        if (descriptor.getAssistants() == null) { return result; }

        for (var entry : descriptor.getAssistants().entrySet()) {
            var id = entry.getKey();
            var assistant = entry.getValue();

            if (!matchesFilter(id, assistantFilter, excludeAssistants)) { continue; }

            // If explicitly selected via --assistants, skip detection
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

    // ──────────────────────────── Install plan ────────────────────────────

    private static List<AgentExtensionsOutputDescriptor> buildInstallPlan(
            AgentExtensionsDistributionDescriptor descriptor,
            Map<String, AgentExtensionsAssistantDescriptor> assistants,
            AgentExtensionsSourceHandler sourceHandler,
            AgentExtensionsConditionEvaluator conditionEvaluator,
            AgentExtensionsInstallPlanContext planContext,
            Set<String> contentTypeFilter,
            String customDir,
            String sourceVersion) {
        // Phase 1: mark all detected assistants' content types as planned
        for (var entry : assistants.entrySet()) {
            var assistant = entry.getValue();
            if (assistant.getTargets() == null) { continue; }
            for (var target : assistant.getTargets()) {
                planContext.markInstalled(entry.getKey(), target.getContentType());
            }
        }

        // Phase 2: build the plan, evaluating skip-if
        var plan = new ArrayList<AgentExtensionsOutputDescriptor>();
        var explicitlySelected = !assistants.isEmpty() && assistants.keySet().stream()
            .anyMatch(k -> true); // simplification: always have detected assistants

        for (var entry : assistants.entrySet()) {
            var assistantId = entry.getKey();
            var assistant = entry.getValue();
            if (assistant.getTargets() == null) { continue; }

            for (var target : assistant.getTargets()) {
                var contentType = target.getContentType();
                if (!matchesContentTypeFilter(contentType, contentTypeFilter)) { continue; }

                var resolvedDir = customDir != null
                    ? Path.of(customDir).toAbsolutePath().normalize()
                    : AgentExtensionsPathResolver.resolve(target.getTargetDir());
                if (resolvedDir == null) { continue; }

                // Evaluate skip-if (skip for explicit --assistants selection)
                boolean skipIfResult = target.getSkipIf() != null
                    && conditionEvaluator.evaluate(target.getSkipIf());
                // Auto-dedup: check if same target dir + content type already processed
                boolean isDuplicate = !planContext.markTargetDir(resolvedDir, contentType);

                var sourceFiles = discoverSourceFiles(descriptor, target, sourceHandler);
                for (var sourceFile : sourceFiles) {
                    var targetPath = resolvedDir.resolve(
                        getTargetRelativePath(descriptor, target, sourceFile));
                    String action;
                    if (skipIfResult) {
                        action = "SKIPPED";
                    } else if (isDuplicate) {
                        action = "SKIPPED";
                    } else {
                        action = "INSTALLED";
                    }
                    plan.add(AgentExtensionsOutputDescriptor.builder()
                        .assistant(assistant.getDisplayName())
                        .assistantId(assistantId)
                        .file(sourceFile)
                        .contentType(contentType)
                        .targetDir(resolvedDir.toString())
                        .targetPath(targetPath.toString())
                        .sourceVersion(sourceVersion)
                        .actionResult(action)
                        .build());
                }
            }
        }
        return plan;
    }

    // ──────────────────────────── Update plan ────────────────────────────

    private static List<AgentExtensionsOutputDescriptor> buildUpdatePlan(
            AgentExtensionsDistributionDescriptor descriptor,
            Map<String, AgentExtensionsAssistantDescriptor> assistants,
            AgentExtensionsSourceHandler sourceHandler,
            AgentExtensionsConditionEvaluator conditionEvaluator,
            AgentExtensionsInstallPlanContext planContext,
            Set<String> contentTypeFilter,
            String customDir,
            String sourceVersion) {
        // First build install plan to know what should exist
        var installPlan = buildInstallPlan(descriptor, assistants, sourceHandler,
            conditionEvaluator, planContext, contentTypeFilter, customDir, sourceVersion);

        // Load existing state to determine what changed
        var existingState = loadAllStateDescriptors();
        var existingByKey = existingState.stream()
            .collect(Collectors.toMap(
                s -> s.getAssistantId() + ":" + s.getFile(),
                s -> s, (a, b) -> a));

        var plan = new ArrayList<AgentExtensionsOutputDescriptor>();
        var handledKeys = new HashSet<String>();

        for (var entry : installPlan) {
            var key = entry.getAssistantId() + ":" + entry.getFile();
            handledKeys.add(key);

            if ("SKIPPED".equals(entry.getActionResult())) {
                plan.add(entry);
                continue;
            }

            var existing = existingByKey.get(key);
            if (existing == null) {
                // New file
                plan.add(AgentExtensionsOutputDescriptor.builder()
                    .assistant(entry.getAssistant())
                    .assistantId(entry.getAssistantId())
                    .file(entry.getFile())
                    .contentType(entry.getContentType())
                    .targetDir(entry.getTargetDir())
                    .targetPath(entry.getTargetPath())
                    .sourceVersion(sourceVersion)
                    .actionResult("INSTALLED")
                    .build());
            } else if (hasFileChanged(sourceHandler, entry.getFile(), Path.of(existing.getTargetPath()))) {
                // Changed file
                plan.add(AgentExtensionsOutputDescriptor.builder()
                    .assistant(entry.getAssistant())
                    .assistantId(entry.getAssistantId())
                    .file(entry.getFile())
                    .contentType(entry.getContentType())
                    .targetDir(entry.getTargetDir())
                    .targetPath(entry.getTargetPath())
                    .sourceVersion(sourceVersion)
                    .actionResult("UPDATED")
                    .build());
            } else {
                // Unchanged
                plan.add(AgentExtensionsOutputDescriptor.builder()
                    .assistant(entry.getAssistant())
                    .assistantId(entry.getAssistantId())
                    .file(entry.getFile())
                    .contentType(entry.getContentType())
                    .targetDir(entry.getTargetDir())
                    .targetPath(entry.getTargetPath())
                    .sourceVersion(sourceVersion)
                    .actionResult("UNCHANGED")
                    .build());
            }
        }

        // Files that exist in state but not in source → REMOVED
        for (var existing : existingState) {
            var key = existing.getAssistantId() + ":" + existing.getFile();
            if (!handledKeys.contains(key)
                    && matchesFilter(existing.getAssistantId(),
                        assistants.isEmpty() ? null : assistants.keySet(), null)) {
                plan.add(AgentExtensionsOutputDescriptor.builder()
                    .assistant(existing.getAssistant())
                    .assistantId(existing.getAssistantId())
                    .file(existing.getFile())
                    .contentType(existing.getContentType())
                    .targetDir(existing.getTargetDir())
                    .targetPath(existing.getTargetPath())
                    .sourceVersion(sourceVersion)
                    .actionResult("REMOVED")
                    .build());
            }
        }

        return plan;
    }

    // ──────────────────────────── Content discovery ────────────────────────────

    private static List<String> discoverSourceFiles(
            AgentExtensionsDistributionDescriptor descriptor,
            AgentExtensionsTargetDescriptor target,
            AgentExtensionsSourceHandler sourceHandler) {
        var contentType = target.getContentType();
        var ctDesc = descriptor.getContentTypes() != null
            ? descriptor.getContentTypes().get(contentType) : null;

        if (ctDesc == null) { return Collections.emptyList(); }

        var discoverMode = ctDesc.getDiscover();
        if ("explicit".equals(discoverMode)) {
            return discoverExplicitEntries(target, sourceHandler);
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
            String sourceDir, String entryMarker, AgentExtensionsSourceHandler sourceHandler) {
        var result = new ArrayList<String>();
        sourceHandler.listDirs(sourceDir).forEach(dir -> {
            if (entryMarker != null) {
                var markerPath = dir.resolve(entryMarker);
                if (!sourceHandler.exists(markerPath.toString())) { return; }
            }
            // Include all files in this directory tree
            sourceHandler.listFiles(dir.toString()).forEach(f -> {
                var relative = sourceHandler.getExtractedDir().relativize(
                    sourceHandler.getExtractedDir().resolve(f));
                result.add(relative.toString());
            });
        });
        return result;
    }

    private static List<String> discoverFileEntries(
            String sourceDir, String filePattern, AgentExtensionsSourceHandler sourceHandler) {
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
            AgentExtensionsTargetDescriptor target, AgentExtensionsSourceHandler sourceHandler) {
        if (target.getSourceEntries() == null) { return Collections.emptyList(); }
        var result = new ArrayList<String>();
        for (var entryDir : target.getSourceEntries()) {
            if (sourceHandler.exists(entryDir)) {
                var entryPath = Path.of(entryDir);
                if (Files.isDirectory(sourceHandler.getExtractedDir().resolve(entryDir))) {
                    sourceHandler.listFiles(entryDir).forEach(f -> result.add(f.toString()));
                } else {
                    result.add(entryDir);
                }
            }
        }
        return result;
    }

    /**
     * Get the relative path for a file within its target directory.
     * For directory-discovered content (skills), preserve the directory structure
     * under the source-dir. For explicit and file-discovered content, use just the filename
     * relative to source-entries dir.
     */
    private static String getTargetRelativePath(
            AgentExtensionsDistributionDescriptor descriptor,
            AgentExtensionsTargetDescriptor target,
            String sourceFile) {
        var contentType = target.getContentType();
        var ctDesc = descriptor.getContentTypes() != null
            ? descriptor.getContentTypes().get(contentType) : null;

        if (ctDesc == null) { return Path.of(sourceFile).getFileName().toString(); }

        var discoverMode = ctDesc.getDiscover();
        if ("explicit".equals(discoverMode) && target.getSourceEntries() != null) {
            // For explicit entries, strip the source-entries prefix
            for (var entryDir : target.getSourceEntries()) {
                if (sourceFile.startsWith(entryDir + "/")) {
                    return sourceFile.substring(entryDir.length() + 1);
                } else if (sourceFile.equals(entryDir)) {
                    return Path.of(sourceFile).getFileName().toString();
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

    // ──────────────────────────── Signature verification ────────────────────────────

    private static void verifyDescriptorSignature(
            AgentExtensionsSourceHandler sourceHandler,
            PolicyAction onInvalidSignature,
            PolicyAction onUnsigned) {
        var manifest = sourceHandler.readManifest();
        if (manifest == null) {
            handlePolicy(onUnsigned, "No manifest.json found — extensions are unsigned");
            return;
        }
        var sig = manifest.get("extensions-distribution.yaml");
        if (sig == null) {
            handlePolicy(onUnsigned, "extensions-distribution.yaml is unsigned (not in manifest)");
            return;
        }
        verifyFileSignature(sourceHandler, "extensions-distribution.yaml", sig, onInvalidSignature);
    }

    private static void verifyPlanSignatures(
            List<AgentExtensionsOutputDescriptor> plan,
            AgentExtensionsSourceHandler sourceHandler,
            PolicyAction onInvalidSignature,
            PolicyAction onUnsigned) {
        var manifest = sourceHandler.readManifest();
        if (manifest == null) {
            // Already handled in verifyDescriptorSignature
            return;
        }
        for (var entry : plan) {
            if ("SKIPPED".equals(entry.getActionResult())) { continue; }
            var sig = manifest.get(entry.getFile());
            if (sig == null) {
                handlePolicy(onUnsigned, "File is unsigned: " + entry.getFile());
                continue;
            }
            verifyFileSignature(sourceHandler, entry.getFile(), sig, onInvalidSignature);
        }
    }

    private static void verifyFileSignature(
            AgentExtensionsSourceHandler sourceHandler, String file,
            String expectedSignature, PolicyAction onInvalidSignature) {
        var fileBytes = sourceHandler.readFileBytes(file);
        if (fileBytes == null) {
            handlePolicy(onInvalidSignature, "File not found for signature verification: " + file);
            return;
        }
        var status = SignatureHelper.fortifySignatureVerifier().verify(fileBytes, expectedSignature);
        if (status != SignatureStatus.VALID) {
            handlePolicy(onInvalidSignature,
                "Invalid signature for " + file + " (status: " + status + ")");
        }
    }

    // ──────────────────────────── Schema version ────────────────────────────

    private static void validateSchemaVersion(String schemaVersion, PolicyAction onInvalidVersion) {
        if (!AgentExtensionsSchemaHelper.isCompatible(schemaVersion)) {
            handlePolicy(onInvalidVersion,
                "Incompatible extensions descriptor schema version: " + schemaVersion
                + " (supported: " + AgentExtensionsSchemaHelper.SUPPORTED_SCHEMA_VERSION + ")"
                + "\n  Consider updating fcli to a newer version.");
        }
    }

    // ──────────────────────────── Policy handling ────────────────────────────

    private static void handlePolicy(PolicyAction action, String message) {
        if (action == null) { action = PolicyAction.warn; }
        switch (action) {
            case ignore -> LOG.debug("Ignored: {}", message);
            case warn   -> LOG.warn("WARNING: {}", message);
            case fail   -> throw new FcliSimpleException(message);
        }
    }

    // ──────────────────────────── Plan execution ────────────────────────────

    private static void executePlan(
            List<AgentExtensionsOutputDescriptor> plan,
            AgentExtensionsSourceHandler sourceHandler) {
        for (var entry : plan) {
            if (!"INSTALLED".equals(entry.getActionResult())) { continue; }
            installFile(sourceHandler, entry);
        }
    }

    private static void executeUpdatePlan(
            List<AgentExtensionsOutputDescriptor> plan,
            AgentExtensionsSourceHandler sourceHandler) {
        for (var entry : plan) {
            switch (entry.getActionResult()) {
                case "INSTALLED", "UPDATED" -> installFile(sourceHandler, entry);
                case "REMOVED" -> {
                    deleteTargetFile(Path.of(entry.getTargetPath()));
                    deleteStateDescriptor(entry.getAssistantId(), entry.getFile());
                }
            }
        }
        cleanEmptyStateDirs();
    }

    private static void installFile(
            AgentExtensionsSourceHandler sourceHandler,
            AgentExtensionsOutputDescriptor entry) {
        var targetPath = Path.of(entry.getTargetPath());
        var sourceBytes = sourceHandler.readFileBytes(entry.getFile());
        if (sourceBytes == null) {
            throw new FcliSimpleException("Source file not found: " + entry.getFile());
        }
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, sourceBytes);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error installing file: " + targetPath, e);
        }
        saveStateDescriptor(entry);
    }

    // ──────────────────────────── State management ────────────────────────────

    private static void saveStateDescriptor(AgentExtensionsOutputDescriptor entry) {
        var stateDescriptor = AgentExtensionsStateDescriptor.builder()
            .assistant(entry.getAssistant())
            .assistantId(entry.getAssistantId())
            .file(entry.getFile())
            .contentType(entry.getContentType())
            .targetDir(entry.getTargetDir())
            .targetPath(entry.getTargetPath())
            .sourceVersion(entry.getSourceVersion())
            .timestamp(Instant.now().toString())
            .build();
        var relativePath = STATE_BASE_PATH
            .resolve(entry.getAssistantId())
            .resolve(entry.getFile() + ".json");
        FcliDataHelper.saveFile(relativePath, stateDescriptor, true);
    }

    private static void deleteStateDescriptor(String assistantId, String file) {
        var relativePath = STATE_BASE_PATH
            .resolve(assistantId)
            .resolve(file + ".json");
        FcliDataHelper.deleteFile(relativePath, false);
    }

    private static void deleteTargetFile(Path targetPath) {
        try {
            Files.deleteIfExists(targetPath);
            // Clean up empty parent directories
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

    static List<AgentExtensionsStateDescriptor> loadAllStateDescriptors() {
        var result = new ArrayList<AgentExtensionsStateDescriptor>();
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
                                .readValue(content, AgentExtensionsStateDescriptor.class);
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
                            if (stream.findAny().isEmpty()) {
                                Files.delete(dir);
                            }
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
            AgentExtensionsSourceHandler sourceHandler, String sourceFile, Path targetPath) {
        if (!Files.isRegularFile(targetPath)) { return true; }
        try {
            var sourceBytes = sourceHandler.readFileBytes(sourceFile);
            var targetBytes = Files.readAllBytes(targetPath);
            return sourceBytes == null || !java.util.Arrays.equals(sourceBytes, targetBytes);
        } catch (IOException e) {
            return true;
        }
    }

    private static boolean matchesContentTypeFilter(String contentType, Set<String> filter) {
        return filter == null || filter.isEmpty() || filter.contains(contentType);
    }
}
