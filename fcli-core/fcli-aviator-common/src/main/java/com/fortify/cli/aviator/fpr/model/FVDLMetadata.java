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
package com.fortify.cli.aviator.fpr.model;


import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

/**
 * Container for FPR metadata extracted during streaming parse.
 */
@Data
public class FVDLMetadata {

    private String buildId;
    private String projectName;
    private String projectVersion;
    private String engineVersion;
    private String analysisType = "SCA";

    // Rule metadata cache: classId -> metadata map
    private Map<String, Map<String, String>> ruleMetadata = new ConcurrentHashMap<>();

    // Node pool: nodeId -> minimal node data
    private Map<String, Node> nodePool = new ConcurrentHashMap<>();

    // Trace pool: traceId -> StreamedTrace
    private Map<String, StreamedTrace> tracePool = new ConcurrentHashMap<>();

    // Context pool: contextId -> parsed context information
    private Map<String, ContextInfo> contextPool = new ConcurrentHashMap<>();

    // Description cache: classID -> StreamedDescription
    private Map<String, StreamedDescription> descriptionCache = new ConcurrentHashMap<>();

    // Source file typing from Build/SourceFiles/File Name + @type
    private Map<String, String> sourceFileTypesByPath = new ConcurrentHashMap<>();
    private Map<String, String> sourceFileTypesByPathIgnoreCase = new ConcurrentHashMap<>();
    private Map<String, String> sourceFileTypesByBaseName = new ConcurrentHashMap<>();
    private Map<String, String> sourceFileTypesByBaseNameIgnoreCase = new ConcurrentHashMap<>();
    private Map<String, String> sourceFileTypesByExtension = new ConcurrentHashMap<>();
    private Set<String> ambiguousSourcePaths = ConcurrentHashMap.newKeySet();
    private Set<String> ambiguousSourcePathsIgnoreCase = ConcurrentHashMap.newKeySet();
    private Set<String> ambiguousSourceBaseNames = ConcurrentHashMap.newKeySet();
    private Set<String> ambiguousSourceBaseNamesIgnoreCase = ConcurrentHashMap.newKeySet();
    private Set<String> ambiguousSourceExtensions = ConcurrentHashMap.newKeySet();

    // Source file encodings from Build/SourceFiles/File Name + @encoding
    private Map<String, String> sourceFileEncodingsByPath = new ConcurrentHashMap<>();
    private Map<String, String> sourceFileEncodingsByPathIgnoreCase = new ConcurrentHashMap<>();
    private Set<String> ambiguousSourceEncodingPaths = ConcurrentHashMap.newKeySet();
    private Set<String> ambiguousSourceEncodingPathsIgnoreCase = ConcurrentHashMap.newKeySet();

    // Statistics
    private long totalVulnerabilities;
    private long totalNodes;
    private long totalTraces;

    public void registerSourceFileType(String fileName, String fileType) {
        if (fileName == null || fileType == null) {
            return;
        }

        String normalizedPath = normalizeFileName(fileName);
        String normalizedType = normalizeType(fileType);
        if (normalizedPath.isEmpty() || normalizedType.isEmpty()) {
            return;
        }

        registerUniqueMapping(sourceFileTypesByPath, ambiguousSourcePaths, normalizedPath, normalizedType);
        registerUniqueMapping(sourceFileTypesByPathIgnoreCase, ambiguousSourcePathsIgnoreCase,
            foldCase(normalizedPath), normalizedType);

        String baseName = extractBaseName(normalizedPath);
        if (!baseName.isEmpty()) {
            registerUniqueMapping(sourceFileTypesByBaseName, ambiguousSourceBaseNames, baseName, normalizedType);
            registerUniqueMapping(sourceFileTypesByBaseNameIgnoreCase, ambiguousSourceBaseNamesIgnoreCase,
                foldCase(baseName), normalizedType);
        }

        String extension = extractExtension(baseName);
        if (!extension.isEmpty()) {
            registerUniqueMapping(sourceFileTypesByExtension, ambiguousSourceExtensions, extension, normalizedType);
        }
    }

    public void registerSourceFileEncoding(String fileName, String encoding) {
        if (fileName == null || encoding == null) {
            return;
        }

        String normalizedPath = normalizeFileName(fileName);
        String normalizedEncoding = normalizeEncoding(encoding);
        if (normalizedPath.isEmpty() || normalizedEncoding.isEmpty()) {
            return;
        }

        registerUniqueMapping(sourceFileEncodingsByPath, ambiguousSourceEncodingPaths, normalizedPath, normalizedEncoding);
        registerUniqueMapping(sourceFileEncodingsByPathIgnoreCase, ambiguousSourceEncodingPathsIgnoreCase,
            foldCase(normalizedPath), normalizedEncoding);
    }

    public String findSourceFileTypeForFileName(String fileName) {
        String normalizedPath = normalizeFileName(fileName);
        if (normalizedPath.isEmpty()) {
            return null;
        }

        String exactPathType = getUniqueMapping(sourceFileTypesByPath, ambiguousSourcePaths, normalizedPath);
        if (exactPathType != null) {
            return exactPathType;
        }

        String foldedPathType = getUniqueMapping(sourceFileTypesByPathIgnoreCase, ambiguousSourcePathsIgnoreCase,
            foldCase(normalizedPath));
        if (foldedPathType != null) {
            return foldedPathType;
        }

        String baseName = extractBaseName(normalizedPath);
        if (baseName.isEmpty()) {
            return null;
        }

        String exactBaseNameType = getUniqueMapping(sourceFileTypesByBaseName, ambiguousSourceBaseNames, baseName);
        if (exactBaseNameType != null) {
            return exactBaseNameType;
        }

        return getUniqueMapping(sourceFileTypesByBaseNameIgnoreCase, ambiguousSourceBaseNamesIgnoreCase,
            foldCase(baseName));
    }

    public String findSourceFileTypeForExtension(String extension) {
        String normalizedExtension = normalizeExtension(extension);
        if (normalizedExtension.isEmpty()) {
            return null;
        }

        return getUniqueMapping(sourceFileTypesByExtension, ambiguousSourceExtensions, normalizedExtension);
    }

    public String findSourceFileEncodingForFileName(String fileName) {
        String normalizedPath = normalizeFileName(fileName);
        if (normalizedPath.isEmpty()) {
            return null;
        }

        String exactPathEncoding = getUniqueMapping(sourceFileEncodingsByPath, ambiguousSourceEncodingPaths, normalizedPath);
        if (exactPathEncoding != null) {
            return exactPathEncoding;
        }

        return getUniqueMapping(sourceFileEncodingsByPathIgnoreCase, ambiguousSourceEncodingPathsIgnoreCase,
            foldCase(normalizedPath));
    }

    private static void registerUniqueMapping(Map<String, String> mappings, Set<String> ambiguousKeys,
                                              String key, String value) {
        if (ambiguousKeys.contains(key)) {
            return;
        }

        String existing = mappings.putIfAbsent(key, value);
        if (existing != null && !existing.equalsIgnoreCase(value)) {
            mappings.remove(key);
            ambiguousKeys.add(key);
        }
    }

    private static String getUniqueMapping(Map<String, String> mappings, Set<String> ambiguousKeys, String key) {
        if (ambiguousKeys.contains(key)) {
            return null;
        }
        return mappings.get(key);
    }

    private static String normalizeFileName(String fileName) {
        String normalized = fileName == null ? "" : fileName.trim().replace('\\', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private static String normalizeType(String fileType) {
        return fileType == null ? "" : fileType.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeEncoding(String encoding) {
        return encoding == null ? "" : encoding.trim();
    }

    private static String extractBaseName(String normalizedPath) {
        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        return lastSlashIndex >= 0 ? normalizedPath.substring(lastSlashIndex + 1) : normalizedPath;
    }

    private static String extractExtension(String baseName) {
        int lastDotIndex = baseName.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == baseName.length() - 1) {
            return "";
        }
        return baseName.substring(lastDotIndex).toLowerCase(Locale.ROOT);
    }

    private static String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }

        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }

        return normalized.startsWith(".") ? normalized : "." + normalized;
    }

    private static String foldCase(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public void clearSourceFileTypeIndexes() {
        sourceFileTypesByPath = new ConcurrentHashMap<>();
        sourceFileTypesByPathIgnoreCase = new ConcurrentHashMap<>();
        sourceFileTypesByBaseName = new ConcurrentHashMap<>();
        sourceFileTypesByBaseNameIgnoreCase = new ConcurrentHashMap<>();
        sourceFileTypesByExtension = new ConcurrentHashMap<>();
        ambiguousSourcePaths = ConcurrentHashMap.newKeySet();
        ambiguousSourcePathsIgnoreCase = ConcurrentHashMap.newKeySet();
        ambiguousSourceBaseNames = ConcurrentHashMap.newKeySet();
        ambiguousSourceBaseNamesIgnoreCase = ConcurrentHashMap.newKeySet();
        ambiguousSourceExtensions = ConcurrentHashMap.newKeySet();
    }


    @Data
    public static class NodeData {
        private String nodeId;
        private String filePath;
        private Integer line;
        private Integer lineEnd;
        private Integer colStart;
        private Integer colEnd;
        private String actionType;
        private String label;
    }

    @Data
    public static class ContextInfo {
        private String id;
        private String namespace;
        private String className;
        private String functionName;
        private String filename;
        private Integer startLine;

        public String getContextString() {
            return joinNonBlank(namespace, className, functionName);
        }

        public String getQualifiedClassName() {
            return joinNonBlank(namespace, className);
        }

        public String getQualifiedFunctionName() {
            if (functionName == null || functionName.isBlank()) {
                return getQualifiedClassName();
            }

            String qualifiedClassName = getQualifiedClassName();
            StringBuilder builder = new StringBuilder();
            if (!qualifiedClassName.isBlank()) {
                builder.append(qualifiedClassName).append('.');
            }
            builder.append(functionName);
            if (!functionName.startsWith("http")) {
                builder.append("()");
            }
            return builder.toString();
        }

        private String joinNonBlank(String... parts) {
            StringJoiner joiner = new StringJoiner(".");
            for (String part : parts) {
                if (part != null && !part.isBlank()) {
                    joiner.add(part);
                }
            }
            return joiner.toString();
        }
    }
}
