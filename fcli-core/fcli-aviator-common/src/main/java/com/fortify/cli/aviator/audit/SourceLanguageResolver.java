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
package com.fortify.cli.aviator.audit;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fortify.cli.aviator.audit.model.File;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.util.FileTypeLanguageMapperUtil;
import com.fortify.cli.aviator.util.FileUtil;
import com.fortify.cli.aviator.util.StringUtil;

class SourceLanguageResolver {
    static final String UNKNOWN_LANGUAGE = "Unknown";

    private final FVDLMetadata sourceFileTypeMetadata;

    SourceLanguageResolver(FVDLMetadata fvdlMetadata) {
        this.sourceFileTypeMetadata = snapshotSourceFileTypeMetadata(fvdlMetadata);
    }

    String resolvePrimaryLanguage(Vulnerability vulnerability) {
        if (vulnerability == null) {
            return UNKNOWN_LANGUAGE;
        }

        Set<String> candidateFileNames = collectCandidateFileNames(vulnerability);
        String fallbackFileType = vulnerability.getFiletype();

        String exactFprLanguage = resolveFromFprExactMatches(candidateFileNames);
        if (!isUnknown(exactFprLanguage)) {
            return exactFprLanguage;
        }

        String extensionFprLanguage = resolveFromFprExtensionMatches(candidateFileNames);
        if (!isUnknown(extensionFprLanguage)) {
            return extensionFprLanguage;
        }

        String fallbackLanguage = normalizeLanguage(fallbackFileType);
        if (!isUnknown(fallbackLanguage)) {
            return fallbackLanguage;
        }

        String yamlLanguage = resolveFromYamlMappings(candidateFileNames);
        return !isUnknown(yamlLanguage) ? yamlLanguage : UNKNOWN_LANGUAGE;
    }

    Set<String> resolveProgrammingLanguages(Vulnerability vulnerability) {
        if (vulnerability == null) {
            return new LinkedHashSet<>();
        }

        Set<String> candidateFileNames = collectCandidateFileNames(vulnerability);
        LinkedHashSet<String> exactLanguages = new LinkedHashSet<>();
        addResolvedFprExactLanguages(exactLanguages, candidateFileNames);
        if (!exactLanguages.isEmpty()) {
            return exactLanguages;
        }

        LinkedHashSet<String> extensionLanguages = new LinkedHashSet<>();
        addResolvedFprExtensionLanguages(extensionLanguages, candidateFileNames);
        if (!extensionLanguages.isEmpty()) {
            return extensionLanguages;
        }

        String fallbackLanguage = normalizeLanguage(vulnerability.getFiletype());
        if (!isUnknown(fallbackLanguage)) {
            LinkedHashSet<String> fallbackLanguages = new LinkedHashSet<>();
            fallbackLanguages.add(fallbackLanguage);
            return fallbackLanguages;
        }

        LinkedHashSet<String> yamlLanguages = new LinkedHashSet<>();
        addResolvedYamlLanguages(yamlLanguages, candidateFileNames);
        if (!yamlLanguages.isEmpty()) {
            return yamlLanguages;
        }

        LinkedHashSet<String> unknownLanguages = new LinkedHashSet<>();
        if (!candidateFileNames.isEmpty()) {
            unknownLanguages.add(UNKNOWN_LANGUAGE);
        }

        return unknownLanguages;
    }

    String resolveLanguage(String fileName, String fallbackFileType) {
        String exactFprLanguage = resolveFromFprExactMatch(fileName);
        if (!isUnknown(exactFprLanguage)) {
            return exactFprLanguage;
        }

        String extensionFprLanguage = resolveFromFprExtension(fileName);
        if (!isUnknown(extensionFprLanguage)) {
            return extensionFprLanguage;
        }

        String fallbackLanguage = normalizeLanguage(fallbackFileType);
        if (!isUnknown(fallbackLanguage)) {
            return fallbackLanguage;
        }

        return resolveFromYaml(fileName);
    }

    String resolvePrimaryFileExtension(Vulnerability vulnerability) {
        if (vulnerability == null) {
            return UNKNOWN_LANGUAGE;
        }

        Set<String> candidateFileNames = collectCandidateFileNames(vulnerability);
        String exactFprExtension = resolveFileExtensionForFirstExactFprMatch(candidateFileNames);
        if (!isUnknown(exactFprExtension)) {
            return exactFprExtension;
        }

        String fprExtension = resolveFileExtensionForFirstFprExtensionMatch(candidateFileNames);
        if (!isUnknown(fprExtension)) {
            return fprExtension;
        }

        String lastStackExtension = normalizeExtension(getFileName(vulnerability.getLastStackTraceElement()));
        if (!isUnknown(lastStackExtension)) {
            return lastStackExtension;
        }

        for (String candidateFileName : candidateFileNames) {
            String candidateExtension = normalizeExtension(candidateFileName);
            if (!isUnknown(candidateExtension)) {
                return candidateExtension;
            }
        }

        return UNKNOWN_LANGUAGE;
    }

    private String resolveFromFprExactMatch(String fileName) {
        if (StringUtil.isEmpty(fileName)) {
            return UNKNOWN_LANGUAGE;
        }

        String sourceFileType = sourceFileTypeMetadata.findSourceFileTypeForFileName(fileName);
        if (!StringUtil.isEmpty(sourceFileType)) {
            return normalizeLanguage(sourceFileType);
        }

        return UNKNOWN_LANGUAGE;
    }

    private String resolveFromFprExtension(String fileName) {
        if (StringUtil.isEmpty(fileName)) {
            return UNKNOWN_LANGUAGE;
        }

        String fileExtension = FileUtil.getFileExtension(fileName);
        String extensionType = sourceFileTypeMetadata.findSourceFileTypeForExtension(fileExtension);
        return normalizeLanguage(extensionType);
    }

    private String resolveFromYaml(String fileName) {
        if (StringUtil.isEmpty(fileName)) {
            return UNKNOWN_LANGUAGE;
        }

        String fileExtension = FileUtil.getFileExtension(fileName);
        return normalizeLanguage(FileTypeLanguageMapperUtil.getProgrammingLanguage(fileExtension));
    }

    private String resolveFromFprExactMatches(Set<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            String resolvedLanguage = resolveFromFprExactMatch(candidateFileName);
            if (!isUnknown(resolvedLanguage)) {
                return resolvedLanguage;
            }
        }
        return UNKNOWN_LANGUAGE;
    }

    private String resolveFromFprExtensionMatches(Set<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            String resolvedLanguage = resolveFromFprExtension(candidateFileName);
            if (!isUnknown(resolvedLanguage)) {
                return resolvedLanguage;
            }
        }
        return UNKNOWN_LANGUAGE;
    }

    private String resolveFromYamlMappings(Set<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            String resolvedLanguage = resolveFromYaml(candidateFileName);
            if (!isUnknown(resolvedLanguage)) {
                return resolvedLanguage;
            }
        }
        return UNKNOWN_LANGUAGE;
    }

    private void addResolvedFprExactLanguages(Set<String> languages, Set<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            addIfKnown(languages, resolveFromFprExactMatch(candidateFileName));
        }
    }

    private void addResolvedFprExtensionLanguages(Set<String> languages, Set<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            addIfKnown(languages, resolveFromFprExtension(candidateFileName));
        }
    }

    private void addResolvedYamlLanguages(Set<String> languages, Set<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            addIfKnown(languages, resolveFromYaml(candidateFileName));
        }
    }

    private String resolveFileExtensionForFirstExactFprMatch(Set<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            if (!isUnknown(resolveFromFprExactMatch(candidateFileName))) {
                return normalizeExtension(candidateFileName);
            }
        }
        return UNKNOWN_LANGUAGE;
    }

    private String resolveFileExtensionForFirstFprExtensionMatch(Set<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            if (!isUnknown(resolveFromFprExtension(candidateFileName))) {
                return normalizeExtension(candidateFileName);
            }
        }
        return UNKNOWN_LANGUAGE;
    }

    private static void addIfKnown(Set<String> languages, String language) {
        if (!isUnknown(language)) {
            languages.add(language);
        }
    }

    private static Set<String> collectCandidateFileNames(Vulnerability vulnerability) {
        LinkedHashSet<String> candidateFileNames = new LinkedHashSet<>();
        if (vulnerability == null) {
            return candidateFileNames;
        }

        addCandidateFileName(candidateFileNames, getFileName(vulnerability.getLastStackTraceElement()));
        if (vulnerability.getFiles() != null) {
            for (File file : vulnerability.getFiles()) {
                if (file != null) {
                    addCandidateFileName(candidateFileNames, file.getName());
                }
            }
        }
        addCandidateFileNamesFromStackTrace(candidateFileNames, vulnerability.getStackTrace());
        return candidateFileNames;
    }

    private static void addCandidateFileName(Set<String> candidateFileNames, String fileName) {
        if (!StringUtil.isEmpty(fileName)) {
            candidateFileNames.add(fileName);
        }
    }

    private static void addCandidateFileNamesFromStackTrace(Set<String> candidateFileNames,
                                                            List<List<StackTraceElement>> stackTraces) {
        if (stackTraces == null) {
            return;
        }

        for (List<StackTraceElement> stackTrace : stackTraces) {
            if (stackTrace == null) {
                continue;
            }
            for (StackTraceElement stackTraceElement : stackTrace) {
                addCandidateFileName(candidateFileNames, getFileName(stackTraceElement));
                if (stackTraceElement != null && stackTraceElement.getInnerStackTrace() != null) {
                    for (StackTraceElement innerStackTraceElement : stackTraceElement.getInnerStackTrace()) {
                        addCandidateFileName(candidateFileNames, getFileName(innerStackTraceElement));
                    }
                }
            }
        }
    }

    private static FVDLMetadata snapshotSourceFileTypeMetadata(FVDLMetadata fvdlMetadata) {
        FVDLMetadata sourceMetadata = new FVDLMetadata();
        if (fvdlMetadata == null) {
            return sourceMetadata;
        }

        sourceMetadata.setSourceFileTypesByPath(new ConcurrentHashMap<>(fvdlMetadata.getSourceFileTypesByPath()));
        sourceMetadata.setSourceFileTypesByPathIgnoreCase(
            new ConcurrentHashMap<>(fvdlMetadata.getSourceFileTypesByPathIgnoreCase()));
        sourceMetadata.setSourceFileTypesByBaseName(new ConcurrentHashMap<>(fvdlMetadata.getSourceFileTypesByBaseName()));
        sourceMetadata.setSourceFileTypesByBaseNameIgnoreCase(
            new ConcurrentHashMap<>(fvdlMetadata.getSourceFileTypesByBaseNameIgnoreCase()));
        sourceMetadata.setSourceFileTypesByExtension(
            new ConcurrentHashMap<>(fvdlMetadata.getSourceFileTypesByExtension()));
        sourceMetadata.setAmbiguousSourcePaths(new HashSet<>(fvdlMetadata.getAmbiguousSourcePaths()));
        sourceMetadata.setAmbiguousSourcePathsIgnoreCase(new HashSet<>(fvdlMetadata.getAmbiguousSourcePathsIgnoreCase()));
        sourceMetadata.setAmbiguousSourceBaseNames(new HashSet<>(fvdlMetadata.getAmbiguousSourceBaseNames()));
        sourceMetadata.setAmbiguousSourceBaseNamesIgnoreCase(
            new HashSet<>(fvdlMetadata.getAmbiguousSourceBaseNamesIgnoreCase()));
        sourceMetadata.setAmbiguousSourceExtensions(new HashSet<>(fvdlMetadata.getAmbiguousSourceExtensions()));
        return sourceMetadata;
    }

    private static String getFileName(StackTraceElement stackTraceElement) {
        return stackTraceElement != null ? stackTraceElement.getFilename() : null;
    }

    private static String normalizeLanguage(String language) {
        if (StringUtil.isEmpty(language)) {
            return UNKNOWN_LANGUAGE;
        }

        String trimmed = language.trim();
        return UNKNOWN_LANGUAGE.equalsIgnoreCase(trimmed)
            ? UNKNOWN_LANGUAGE
            : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeExtension(String fileName) {
        String extension = FileUtil.getFileExtension(fileName);
        return StringUtil.isEmpty(extension) ? UNKNOWN_LANGUAGE : extension.toLowerCase(Locale.ROOT);
    }

    private static boolean isUnknown(String language) {
        return UNKNOWN_LANGUAGE.equalsIgnoreCase(normalizeLanguage(language));
    }
}