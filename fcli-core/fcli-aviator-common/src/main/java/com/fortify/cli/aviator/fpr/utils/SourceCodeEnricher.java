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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.audit.model.File;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
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

    /*private final Path extractedPath;
    private final Map<String, String> sourceFileMap;*/
    private final FprHandle fprHandle;
    private final FileUtils fileUtils;

    /**
     * Creates a new SourceCodeEnricher with the required dependencies.
     * @param fprHandle      Utility for file operations (line numbering, line counting)
     */
    /*public SourceCodeEnricher(Path extractedPath, Map<String, String> sourceFileMap, FileUtils fileUtils) {
        this.extractedPath = extractedPath;
        this.sourceFileMap = sourceFileMap;
        this.fileUtils = fileUtils;
    }*/

    public SourceCodeEnricher(FprHandle fprHandle){
        this.fprHandle = fprHandle;
        this.fileUtils = new FileUtils();
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
            String internalPath = fprHandle.getSourceFileMap().get(filename);
            if (internalPath == null) { return; } // Should not happen due to containsKey check, but safe.

            Path actualSourcePath = fprHandle.getPath("/" + internalPath);

            File file = new File();
            file.setName(filename);
            file.setSegment(false);
            file.setStartLine(1);

            try {
                if (Files.exists(actualSourcePath)) {
                    byte[] encodedBytes = Files.readAllBytes(actualSourcePath);
                    file.setContent(new String(encodedBytes));
                    file.setEndLine(fileUtils.countLines(actualSourcePath));
                } else {
                    // This warning is now more accurate.
                    logger.warn("Source file not found at internal path: {}. This may indicate a corrupt FPR.", actualSourcePath);
                    file.setContent("");
                    file.setEndLine(0);
                }
            } catch (IOException e) {
                logger.warn("Error processing file: {}", filename, e);
                file.setContent("");
                file.setEndLine(0);
            }
            uniqueFiles.put(filename, file);
        }
    }
}
