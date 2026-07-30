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
package com.fortify.cli.aviator.fpr.utils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.audit.model.Fragment;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.util.FileTypeLanguageMapperUtil;
import com.fortify.cli.aviator.util.FileUtil;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.LanguageCommentMapperUtil;
import com.fortify.cli.aviator.util.StringUtil;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    private final Map<Path, List<String>> fileContentCache = new ConcurrentHashMap<>();
    private final SourceEncodingOptions sourceEncodingOptions;
    private final FVDLMetadata fvdlMetadata;

    public FileUtils() {
        this(SourceEncodingOptions.defaults(), null);
    }

    public FileUtils(SourceEncodingOptions sourceEncodingOptions, FVDLMetadata fvdlMetadata) {
        this.sourceEncodingOptions = sourceEncodingOptions == null ? SourceEncodingOptions.defaults() : sourceEncodingOptions;
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
                byte[] fileBytes = Files.readAllBytes(path);
                String content = sourceEncodingOptions.decode(fileBytes, filename, fvdlMetadata).content();
                return Arrays.asList(content.split("\\r?\\n"));
            } catch (IOException | SourceEncodingOptions.SourceDecodeException e) {
                logger.error("Failed to read file: {}", path, e);
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
        Path fullSourcePath = resolveFullPath(fprHandle, relativePath);
        if (fullSourcePath == null) return "";

        List<String> lines = readFileWithFallback(fullSourcePath, relativePath);
        if (lineNumber > 0 && lines.size() >= lineNumber) {
            return appendLineNumbers(lines.get(lineNumber - 1), relativePath, lineNumber - 1);
        }
        logger.info("Could not get line {} from file {} (total lines: {})", lineNumber, fullSourcePath, lines.size());
        return "";
    }

    /**
     * Extracts a code fragment (a few lines of code) from a file.
     */
    public Fragment getFragmentFromFile(FprHandle fprHandle, String relativePath, int lineNumber, int linesBefore, int linesAfter) {
        Path fullSourcePath = resolveFullPath(fprHandle, relativePath);
        if (fullSourcePath == null) {
            return new Fragment("", 0, 0);
        }

        List<String> lines = readFileWithFallback(fullSourcePath, relativePath);
        if (lines.isEmpty() || lineNumber <= 0) {
            return new Fragment("", 0, 0);
        }

        int startLine = Math.max(1, lineNumber - linesBefore) + 1;
        int endLine = Math.min(lines.toArray().length - 1, lineNumber + linesAfter) + 1;

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
            logger.debug("Source file key not found in sourceFileMap: {}", relativePath);
            return null;
        }
        // Gets the Path from the virtual file system
        return fprHandle.getPath(internalPath);
    }


    // Assumes you have already updated the signature to accept extractedPath
    public Optional<String> getSourceFileContent(FprHandle fprHandle, String relativePath) {
        Path actualSourcePath = resolveFullPath(fprHandle, relativePath);
        if (actualSourcePath == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(String.join(System.lineSeparator(), readFileWithFallback(actualSourcePath, relativePath)));
        } catch (Exception e) {
            logger.warn("Could not read source file content for path: {}", relativePath, e);
            return Optional.empty();
        }
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
            logger.warn("No Comment symbol is there so line numbers not appended");
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
