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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.audit.model.File;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.StringUtil;


/**
 * Utility class for enriching stack traces with source code files.
 * Extracts unique files from stack traces and loads their content from the FPR extraction directory.
 *
 * This class provides the same logic as FVDLProcessor.processStackTraceElements() but as a
 * reusable, injectable component that can be used in different contexts (e.g., just-in-time
 * enrichment before sending to LLM).
 */
public class SourceCodeEnricher {
    private static final Logger logger = LoggerFactory.getLogger(SourceCodeEnricher.class);

    private final FprHandle fprHandle;
    private final FileUtils fileUtils;
    private final Map<String, CachedSourceResult> sourceFileCache = new ConcurrentHashMap<>();

    public SourceCodeEnricher(FprHandle fprHandle) {
        this(fprHandle, SourceDecoders.defaults(), null);
    }

    public SourceCodeEnricher(FprHandle fprHandle, ISourceDecoder sourceDecoder, FVDLMetadata fvdlMetadata) {
        this.fprHandle = fprHandle;
        this.fileUtils = new FileUtils(Objects.requireNonNull(sourceDecoder, "sourceDecoder"), fvdlMetadata);
    }

    /**
     * Enriches stack traces with source code files.
     * Extracts unique files from all stack traces (including inner traces) and loads their content.
     *
     * This method processes:
     * - All stack traces in the list
     * - All elements in each stack trace
     * - All inner stack traces recursively
     *
     * Files are deduplicated - each unique filename is only loaded once.
     *
     * @param stackTraces List of stack traces to process
     * @return Map of filename → File objects with content loaded
     */
    public Map<String, File> enrichWithSourceCode(List<List<StackTraceElement>> stackTraces) {
        return new HashMap<>(enrichWithSourceCodeDetailed(stackTraces).files());
    }

    public EnrichmentResult enrichWithSourceCodeDetailed(List<List<StackTraceElement>> stackTraces) {
        Map<String, File> uniqueFiles = new LinkedHashMap<>();
        Map<String, SourceFileFailure> failuresByFilename = new LinkedHashMap<>();

        if (stackTraces == null || stackTraces.isEmpty()) {
            logger.debug("No stack traces to enrich");
            return new EnrichmentResult(uniqueFiles, List.of());
        }

        processStackTraces(stackTraces, uniqueFiles, failuresByFilename);

        logger.debug("Enriched {} unique source files from {} stack traces",
                    uniqueFiles.size(), stackTraces.size());

        return new EnrichmentResult(uniqueFiles, new ArrayList<>(failuresByFilename.values()));
    }

    /**
     * Processes all stack traces to extract and load unique source files.
     * Replicates FVDLProcessor.processStackTraceElements() logic.
     */
    private void processStackTraces(List<List<StackTraceElement>> stackTraces, Map<String, File> uniqueFiles,
                                    Map<String, SourceFileFailure> failuresByFilename) {
        for (List<StackTraceElement> stackTrace : stackTraces) {
            if (stackTrace == null) continue;

            for (StackTraceElement element : stackTrace) {
                processFileForElement(element, uniqueFiles, failuresByFilename);

                if (element.getInnerStackTrace() != null) {
                    for (StackTraceElement innerElement : element.getInnerStackTrace()) {
                        processFileForElement(innerElement, uniqueFiles, failuresByFilename);
                    }
                }
            }
        }
    }

    /**
     * Processes a single stack trace element to extract and load its source file.
     * Replicates FVDLProcessor.processFileForElement() logic.
     *
     * @param element     The stack trace element to process
     * @param uniqueFiles Map to store loaded files (deduplicated by filename)
     */
    private void processFileForElement(StackTraceElement element, Map<String, File> uniqueFiles,
                                       Map<String, SourceFileFailure> failuresByFilename) {
        if (element == null) return;

        String filename = element.getFilename();
        if (!StringUtil.isEmpty(filename) && fprHandle.getSourceFileMap().containsKey(filename)
                && !uniqueFiles.containsKey(filename) && !failuresByFilename.containsKey(filename)) {
            CachedSourceResult result = sourceFileCache.computeIfAbsent(filename, this::loadSourceFile);
            if (result.failure() != null) {
                failuresByFilename.put(filename, result.failure());
            } else {
                uniqueFiles.put(filename, result.sourceFile().toFile(filename));
            }
        }
    }

    private CachedSourceResult loadSourceFile(String filename) {
        try {
            String content = fileUtils.readSourceFileContentStrict(fprHandle, filename);
            CachedSourceFile sourceFile = new CachedSourceFile(
                    fileUtils.appendLineNumbers(content, filename, 0), content.split("\\R", -1).length);
            return new CachedSourceResult(sourceFile, null);
        } catch (IOException | ISourceDecoder.SourceDecodeException e) {
            logger.warn("Could not read source file content for path {}: {}", filename, e.getMessage());
            return new CachedSourceResult(null, new SourceFileFailure(filename, e.getMessage()));
        }
    }

    private record CachedSourceFile(String content, int endLine) {
        private File toFile(String filename) {
            File file = new File();
            file.setName(filename);
            file.setSegment(false);
            file.setStartLine(1);
            file.setContent(content);
            file.setEndLine(endLine);
            return file;
        }
    }

    private record CachedSourceResult(CachedSourceFile sourceFile, SourceFileFailure failure) {}

    public record EnrichmentResult(Map<String, File> files, List<SourceFileFailure> failures) {
        public boolean hasFailures() {
            return !failures.isEmpty();
        }
    }

    public record SourceFileFailure(String filename, String message) {}
}
