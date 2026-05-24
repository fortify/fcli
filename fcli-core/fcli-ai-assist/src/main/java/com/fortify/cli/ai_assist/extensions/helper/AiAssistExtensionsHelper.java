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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsSourceHandler.DigestMismatchAction;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

/**
 * Public API for AI assistant extensions operations: setup, uninstall, and
 * listing. Orchestrates {@link AiAssistExtensionsInstallPlanHelper},
 * {@link AiAssistExtensionsStateHelper}, and {@link AiAssistExtensionsContentHelper}.
 */
public final class AiAssistExtensionsHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AiAssistExtensionsHelper.class);

    private AiAssistExtensionsHelper() {}

    // ──────────────────────────── Version resolution ────────────────────────────

    public static ToolDefinitionRootDescriptor getToolDefinitions() {
        return ToolDefinitionsHelper.getToolDefinitionRootDescriptor(
            AiAssistExtensionsSourceHandler.TOOL_NAME);
    }

    public static ToolDefinitionVersionDescriptor resolveVersion(String version) {
        return getToolDefinitions().getVersionOrDefault(version);
    }

    // ──────────────────────────── Setup ────────────────────────────

    public static List<AiAssistExtensionsOutputDescriptor> setup(
            String source, String version,
            Set<String> assistants, boolean autoDetect,
            Set<String> contentTypeFilter, String customDir,
            DigestMismatchAction onDigestMismatch, boolean dryRun) {

        try (var sourceHandler = resolveSource(source, version, onDigestMismatch)) {
            var contentManifest = sourceHandler.readContentManifest();

            if (customDir != null) {
                return setupCustomDir(contentManifest, sourceHandler, contentTypeFilter,
                    customDir, sourceHandler.getVersion(), dryRun);
            }

            var distribution = AiAssistExtensionsSourceHandler
                .readDistributionDescriptor(source == null);
            var selectedAssistants = selectAssistants(distribution, assistants, autoDetect);
            var planContext = new AiAssistExtensionsInstallPlanHelper.PlanContext();
            var plan = AiAssistExtensionsInstallPlanHelper.buildSetupPlan(contentManifest,
                distribution, selectedAssistants, sourceHandler, planContext,
                contentTypeFilter, sourceHandler.getVersion());

            warnDuplicateContentDirs(distribution, selectedAssistants, contentTypeFilter);

            if (!dryRun) {
                AiAssistExtensionsInstallPlanHelper.executePlan(plan, sourceHandler);
                AiAssistExtensionsStateHelper.saveInstallationsState(selectedAssistants, plan);
            }
            return AiAssistExtensionsInstallPlanHelper.toOutputDescriptors(plan);
        }
    }

    private static List<AiAssistExtensionsOutputDescriptor> setupCustomDir(
            AiAssistExtensionsContentManifestDescriptor contentManifest,
            AiAssistExtensionsSourceHandler sourceHandler,
            Set<String> contentTypeFilter, String customDir,
            String sourceVersion, boolean dryRun) {
        var plan = AiAssistExtensionsInstallPlanHelper.buildCustomDirPlan(contentManifest,
            sourceHandler, contentTypeFilter, customDir, sourceVersion);
        if (!dryRun) {
            AiAssistExtensionsInstallPlanHelper.executePlan(plan, sourceHandler);
        }
        return AiAssistExtensionsInstallPlanHelper.toOutputDescriptors(plan);
    }

    // ──────────────────────────── Uninstall ────────────────────────────

    public static List<AiAssistExtensionsOutputDescriptor> uninstall(
            Set<String> contentTypeFilter, String customDir, boolean dryRun) {
        var targetDirs = customDir != null
            ? Set.of(Path.of(customDir).toAbsolutePath().normalize())
            : AiAssistExtensionsStateHelper.collectAllKnownTargetDirs();
        var results = new ArrayList<AiAssistExtensionsOutputDescriptor>();

        for (var dir : targetDirs) {
            for (var manifest : AiAssistExtensionsStateHelper.readAllTargetDirManifests(dir)) {
                if (!AiAssistExtensionsStateHelper.matchesContentTypeFilter(
                        manifest.getContentType(), contentTypeFilter)) {
                    continue;
                }

                var files = manifest.getFiles() != null ? manifest.getFiles() : List.<String>of();
                if (!dryRun) {
                    for (var file : files) {
                        AiAssistExtensionsStateHelper.deleteTargetFile(
                            AiAssistExtensionsStateHelper.safeResolve(dir, file));
                    }
                    AiAssistExtensionsStateHelper.deleteManifestFile(dir, manifest.getContentType());
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
            AiAssistExtensionsStateHelper.clearInstallationsState(contentTypeFilter);
        }
        return results;
    }

    // ──────────────────────────── List installed ────────────────────────────

    public static List<AiAssistExtensionsOutputDescriptor> listInstalled() {
        var installations = AiAssistExtensionsStateHelper.loadInstallationsState();
        var results = new ArrayList<AiAssistExtensionsOutputDescriptor>();

        for (var entry : installations.getAssistants().entrySet()) {
            var assistantId = entry.getKey();
            var installation = entry.getValue();
            for (var targetEntry : installation.getTargets().entrySet()) {
                var contentType = targetEntry.getKey();
                var targetDir = Path.of(targetEntry.getValue());
                var manifest = AiAssistExtensionsStateHelper.readTargetDirManifest(targetDir, contentType);
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

        var installations = AiAssistExtensionsStateHelper.loadInstallationsState();

        var result = new ArrayList<AiAssistExtensionsAssistantOutputDescriptor>();
        for (var entry : distribution.getAssistants().entrySet()) {
            var id = entry.getKey();
            var assistant = entry.getValue();
            var contentTypes = assistant.getTargets() != null
                ? assistant.getTargets().stream()
                    .map(AiAssistExtensionsTargetDescriptor::getContentType)
                    .toArray(String[]::new)
                : new String[0];

            String detected = detect
                ? String.valueOf(AiAssistExtensionsConditionEvaluator.evaluate(assistant.getIfCondition()))
                : "N/A";

            var assistantInstallation = installations.getAssistants().get(id);
            var installed = assistantInstallation != null;
            String installedVersion = null;
            if (installed) {
                installedVersion = assistantInstallation.getTargets().entrySet().stream()
                    .map(e -> AiAssistExtensionsStateHelper.readTargetDirManifest(
                        Path.of(e.getValue()), e.getKey()))
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
            for (var entry : distribution.getAssistants().entrySet()) {
                if (AiAssistExtensionsConditionEvaluator.evaluate(entry.getValue().getIfCondition())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    // ──────────────────────────── Duplicate content warning ────────────────────────────

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
                if (!AiAssistExtensionsStateHelper.matchesContentTypeFilter(
                        contentType, contentTypeFilter)) { continue; }

                var resolvedDirs = AiAssistExtensionsPathResolver.resolveAll(target.getTargetDirs());
                var dirsWithManifest = resolvedDirs.stream()
                    .filter(dir -> AiAssistExtensionsStateHelper.readTargetDirManifest(dir, contentType) != null
                        || selectedAssistants.containsKey(assistantId))
                    .filter(dir -> AiAssistExtensionsStateHelper.readTargetDirManifest(dir, contentType) != null)
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
}
