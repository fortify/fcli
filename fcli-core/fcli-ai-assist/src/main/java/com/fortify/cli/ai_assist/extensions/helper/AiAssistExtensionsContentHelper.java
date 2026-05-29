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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discovers source files from extension content manifests and computes
 * target-relative paths for installation.
 */
final class AiAssistExtensionsContentHelper {
    private AiAssistExtensionsContentHelper() {}

    // ──────────────────────────── Content discovery ────────────────────────────

    static List<String> discoverSourceFiles(
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

    static List<String> discoverSourceFilesForContentType(
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
        var matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
        sourceHandler.listFiles(sourceDir).forEach(f -> {
            if (matcher.matches(f.getFileName())) {
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

    // ──────────────────────────── Target-relative path computation ────────────────────────────

    static String getTargetRelativePath(
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

    static String getTargetRelativePathForContentType(
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
}
