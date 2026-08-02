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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    public SourceCodeEnricher(FprHandle fprHandle) {
        this(fprHandle, SourceDecoders.defaults(), null);
    }

    public SourceCodeEnricher(FprHandle fprHandle, ISourceDecoder sourceDecoder, FVDLMetadata fvdlMetadata) {
        this.fprHandle = fprHandle;
        // Single soft-fail decode path via FileUtils (same policy as snippets/lines).
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
        Map<String, File> uniqueFiles = new HashMap<>();

        if (stackTraces == null || stackTraces.isEmpty()) {
            logger.debug("No stack traces to enrich");
            return uniqueFiles;
        }

        processStackTraces(stackTraces, uniqueFiles);

        logger.debug("Enriched {} unique source files from {} stack traces",
                    uniqueFiles.size(), stackTraces.size());

        return uniqueFiles;
    }

    /**
     * Processes all stack traces to extract and load unique source files.
     * Replicates FVDLProcessor.processStackTraceElements() logic.
     */
    private void processStackTraces(List<List<StackTraceElement>> stackTraces, Map<String, File> uniqueFiles) {
        for (List<StackTraceElement> stackTrace : stackTraces) {
            if (stackTrace == null) continue;

            for (StackTraceElement element : stackTrace) {
                processFileForElement(element, uniqueFiles);

                // Process inner stack traces recursively
                if (element.getInnerStackTrace() != null) {
                    for (StackTraceElement innerElement : element.getInnerStackTrace()) {
                        processFileForElement(innerElement, uniqueFiles);
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
    private void processFileForElement(StackTraceElement element, Map<String, File> uniqueFiles) {
        if (element == null) return;

        String filename = element.getFilename();
        if (!StringUtil.isEmpty(filename) && fprHandle.getSourceFileMap().containsKey(filename) && !uniqueFiles.containsKey(filename)) {
            // Soft-fail decode via FileUtils: omit file rather than fail the whole issue.
            Optional<String> contentOpt = fileUtils.getSourceFileContent(fprHandle, filename);
            if (contentOpt.isEmpty()) {
                return;
            }
            String content = contentOpt.get();
            File file = new File();
            file.setName(filename);
            file.setSegment(false);
            file.setStartLine(1);
            // Keep line markers in prompt file content; downstream gRPC/template rendering is pass-through.
            file.setContent(fileUtils.appendLineNumbers(content, filename, 0));
            file.setEndLine(content.split("\\R", -1).length);
            uniqueFiles.put(filename, file);
        }
    }
}
