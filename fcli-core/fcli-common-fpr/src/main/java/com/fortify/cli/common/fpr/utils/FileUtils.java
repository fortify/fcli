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
package com.fortify.cli.common.fpr.utils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.fpr.model.FVDLMetadata;
import com.fortify.cli.common.fpr.model.Fragment;
import com.fortify.cli.common.fpr.util.*;

/**
 * Source file helpers. Decode failures are soft (empty result + log): snippets/lines are best-effort.
 * Callers that must fail or skip with metrics (e.g. remediation apply) should decode themselves.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    private static final Fragment EMPTY_FRAGMENT = new Fragment("", 0, 0);
    private final Map<Path, List<String>> fileContentCache = new ConcurrentHashMap<>();
    // Many trace nodes share the same file/line; share the immutable results instead of copying text per node.
    private final Map<FragmentKey, Fragment> fragmentCache = new ConcurrentHashMap<>();
    private final Map<LineKey, String> lineCache = new ConcurrentHashMap<>();
    private final Set<String> languagesWithoutCommentSymbol = ConcurrentHashMap.newKeySet();
    private final ISourceDecoder sourceDecoder;
    private final FVDLMetadata fvdlMetadata;

    private record LineKey(String relativePath, int lineNumber) {}
    private record FragmentKey(String relativePath, int lineNumber, int linesBefore, int linesAfter) {}

    public FileUtils() {
        this(SourceDecoders.defaults(), null);
    }

    public FileUtils(ISourceDecoder sourceDecoder, FVDLMetadata fvdlMetadata) {
        this.sourceDecoder = Objects.requireNonNull(sourceDecoder, "sourceDecoder");
        this.fvdlMetadata = fvdlMetadata;
    }

    /**
     * Reads all lines from a file, caching the result to avoid repeated reads.
     * Falls back to empty list on failure, with logging.
     *
     * @param filePath Path to the file
     * @return List of lines, or empty list if file not found or error occurs
     */
    public List<String> readFileWithFallback(Path filePath) {
        return readFileWithFallback(filePath, filePath.getFileName().toString());
    }

    private List<String> readFileWithFallback(Path filePath, String filename) {
        return fileContentCache.computeIfAbsent(filePath, path -> {
            try {
                long fileSize = Files.size(path);
                if (fileSize > FprConstants.MAX_SOURCE_FILE_SIZE) {
                    logger.warn("Source file exceeds maximum allowed size ({} bytes): {} (actual size: {} bytes)",
                        FprConstants.MAX_SOURCE_FILE_SIZE, path, fileSize);
                    return Collections.emptyList();
                }
                byte[] fileBytes = Files.readAllBytes(path);
                String content = sourceDecoder.decode(fileBytes, filename, fvdlMetadata).content();
                return Arrays.asList(content.split("\\r?\\n"));
            } catch (IOException | ISourceDecoder.SourceDecodeException e) {
                logger.warn("Could not read or decode source file {}: {}", path, e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Counts lines in a file using cached content.
     *
     * @param filePath Path to the file
     * @return Number of lines, or 0 if file not found or error
     */
    public int countLines(Path filePath) {
        try {
            return readFileWithFallback(filePath).size();
        } catch (Exception e) {
            logger.error("Error counting lines in file: {}", filePath, e);
            return 0;
        }
    }

    /**
     * Extracts a single line from a file's content.
     */
    public String getLineFromFile(FprHandle fprHandle, String relativePath, int lineNumber) {
        if (relativePath == null) return "";
        return lineCache.computeIfAbsent(new LineKey(relativePath, lineNumber),
            k -> readLineFromFile(fprHandle, relativePath, lineNumber));
    }

    private String readLineFromFile(FprHandle fprHandle, String relativePath, int lineNumber) {
        Path fullSourcePath = resolveFullPath(fprHandle, relativePath);
        if (fullSourcePath == null) return "";

        List<String> lines = readFileWithFallback(fullSourcePath, relativePath);
        if (lineNumber > 0 && lines.size() >= lineNumber) {
            return appendLineNumbers(lines.get(lineNumber - 1), relativePath, lineNumber - 1);
        }
        logger.debug("Could not get line {} from file {} (total lines: {})", lineNumber, fullSourcePath, lines.size());
        return "";
    }

    /**
     * Extracts a code fragment (a few lines of code) from a file.
     */
    public Fragment getFragmentFromFile(FprHandle fprHandle, String relativePath, int lineNumber, int linesBefore, int linesAfter) {
        if (relativePath == null) return EMPTY_FRAGMENT;
        return fragmentCache.computeIfAbsent(new FragmentKey(relativePath, lineNumber, linesBefore, linesAfter),
            k -> readFragmentFromFile(fprHandle, relativePath, lineNumber, linesBefore, linesAfter));
    }

    private Fragment readFragmentFromFile(FprHandle fprHandle, String relativePath, int lineNumber, int linesBefore, int linesAfter) {
        Path fullSourcePath = resolveFullPath(fprHandle, relativePath);
        if (fullSourcePath == null) {
            return EMPTY_FRAGMENT;
        }

        List<String> lines = readFileWithFallback(fullSourcePath, relativePath);
        if (lines.isEmpty() || lineNumber <= 0) {
            return EMPTY_FRAGMENT;
        }

        int startLine = Math.max(1, lineNumber - linesBefore) + 1;
        int endLine = Math.min(lines.size() - 1, lineNumber + linesAfter) + 1;

        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < endLine; i++) {
            sb.append(lines.get(i)).append(System.lineSeparator());
        }

        return new Fragment(appendLineNumbers(sb.toString(), relativePath, startLine - 1), startLine, endLine);
    }

    /**
     * Helper to resolve the full, absolute path to a source file.
     * @return Full path or null if mapping doesn't exist.
     */
    private Path resolveFullPath(FprHandle fprHandle, String relativePath) {
        String internalPath = fprHandle.getSourceFileMap().get(relativePath);
        if (internalPath == null) {
            logger.trace("Source file key not found in sourceFileMap: {}", relativePath);
            return null;
        }
        // Gets the Path from the virtual file system
        return fprHandle.getPath(internalPath);
    }

    public Optional<String> getSourceFileContent(FprHandle fprHandle, String relativePath) {
        try {
            return Optional.of(readSourceFileContentStrict(fprHandle, relativePath));
        } catch (IOException | ISourceDecoder.SourceDecodeException e) {
            logger.warn("Could not read source file content for path {}: {}", relativePath, e.getMessage());
            return Optional.empty();
        }
    }

    /** Reads and decodes source content while preserving read or decode failures for the caller. */
    public String readSourceFileContentStrict(FprHandle fprHandle, String relativePath) throws IOException {
        Path actualSourcePath = resolveFullPath(fprHandle, relativePath);
        if (actualSourcePath == null) {
            throw new IOException("Source file key not found in sourceFileMap: " + relativePath);
        }

        long fileSize = Files.size(actualSourcePath);
        if (fileSize > FprConstants.MAX_SOURCE_FILE_SIZE) {
            throw new IOException("Source file exceeds maximum allowed size (" + FprConstants.MAX_SOURCE_FILE_SIZE
                + " bytes): " + relativePath + " (actual size: " + fileSize + " bytes)");
        }

        byte[] fileBytes = Files.readAllBytes(actualSourcePath);
        return sourceDecoder.decode(fileBytes, relativePath, fvdlMetadata).content();
    }

    public String appendLineNumbers(String content, String fileName, int startLineNo) {
        return appendLineNumbers(content, fileName, startLineNo, null);
    }

    public String appendLineNumbers(String content, String fileName, int startLineNo, String resolvedLanguage) {
        String language = StringUtil.isEmpty(resolvedLanguage) || "Unknown".equalsIgnoreCase(resolvedLanguage)
                ? FileTypeLanguageMapperUtil.getProgrammingLanguage(FileUtil.getFileExtension(fileName))
                : resolvedLanguage;
        String commentSymbol = LanguageCommentMapperUtil.getProgrammingLanguageComment(language);
        if(commentSymbol.equals("Unknown")) {
            if (languagesWithoutCommentSymbol.add(String.valueOf(language))) {
                logger.warn("No comment symbol for language {}; line numbers not appended", language);
            }
            return content;
        }
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\\R", -1); // keep trailing empty lines
        int endLineNo = startLineNo + lines.length;
        for (int i = startLineNo; i < endLineNo; i++) {
            result.append(lines[i-startLineNo]).append(" ").append(commentSymbol).append(" L").append(i + 1);
            if(commentSymbol.equals("<!--"))
                result.append(" -->");
            else if(commentSymbol.equals("<%--"))
                result.append(" --%>");
            if(i!=endLineNo-1)
                result.append(System.lineSeparator());
        }
        return result.toString();
    }
}
