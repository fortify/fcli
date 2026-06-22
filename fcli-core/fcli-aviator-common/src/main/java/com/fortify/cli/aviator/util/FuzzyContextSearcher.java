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
package com.fortify.cli.aviator.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FuzzyContextSearcher {

    /**
     * Performs a fuzzy search of the given context lines inside a source file.
     *
     * @param sourceLines     The List of lines from source file to search in.
     * @param contextLines   The list of lines from the context block.
     * @param maxMismatches  Maximum allowed mismatches (line count-wise) between context and file.
     * @return The line number (0-based) in sourceFile where context starts, or -1 if not found.
     */

    public static int fuzzySearchContext(List<String> sourceLines, List<String> contextLines, int maxMismatches) throws IOException {
        List<String> normalizedSource = normalizeLines(sourceLines);
        List<String> normalizedContext = normalizeLines(contextLines);

        for (int i = 0; i < normalizedSource.size(); i++) {
            int mismatchCount = 0;
            int sourceIndex = i;
            int contextIndex = 0;
            boolean similar = false;

            while (contextIndex < normalizedContext.size() && sourceIndex < normalizedSource.size()) {
                String contextLine = normalizedContext.get(contextIndex).trim();

                if (contextLine.isEmpty()) {
                    contextIndex++; // Skip empty context lines
                    continue;
                }

                // Skip empty source lines too
                String sourceLine = normalizedSource.get(sourceIndex).trim();
                while (sourceLine.isEmpty()) {
                    sourceIndex++;
                    if (sourceIndex >= normalizedSource.size()) {
                        break;
                    }
                    sourceLine = normalizedSource.get(sourceIndex).trim();
                    if(!similar)
                        i = sourceIndex;
                }

                if (sourceIndex >= normalizedSource.size()) {
                    break; // No more source lines to match
                }

                similar = linesSimilar(sourceLine, contextLine);

                if (!similar) {
                    mismatchCount++;
                    if (mismatchCount > maxMismatches) {
                        break;
                    }
                }

                sourceIndex++;
                contextIndex++;
            }

            if (contextIndex == normalizedContext.size() && mismatchCount <= maxMismatches) {
                return i; // Found approximate match starting at i (ignoring blanks)
            }
        }

        return -1; // Not found
    }

    public static int[] fuzzySearchOriginalCode(List<String> sourceLines, List<String> originalCodeLine, int maxMismatches, int startIndex) {
        List<String> normalizedSource = normalizeLines(sourceLines);
        List<String> normalizedOriginalCode = normalizeLines(originalCodeLine);

        for (int i = Math.max(0, startIndex); i < normalizedSource.size(); i++) {
            if (normalizedSource.get(i).isEmpty()) {
                continue;
            }

            int lineTo = findOriginalCodeEnd(normalizedSource, normalizedOriginalCode, maxMismatches, i);
            if (lineTo != -1) {
                return new int[] {i, lineTo};
            }
        }

        return new int[] {-1, -1};
    }

    private static int findOriginalCodeEnd(List<String> normalizedSource, List<String> normalizedOriginalCode, int maxMismatches,
            int sourceIndex) {
        int mismatches = 0;
        int lineTo = -1;
        boolean matchedAnyLine = false;

        for (String originalCodeLine : normalizedOriginalCode) {
            if (originalCodeLine.isEmpty()) {
                continue;
            }

            if (matchedAnyLine) {
                sourceIndex = skipEmptySourceLines(normalizedSource, sourceIndex);
            }
            if (sourceIndex >= normalizedSource.size()) {
                return -1;
            }

            if (!linesSimilar(normalizedSource.get(sourceIndex), originalCodeLine)) {
                mismatches++;
                if (mismatches > maxMismatches) {
                    return -1;
                }
            }

            lineTo = sourceIndex++;
            matchedAnyLine = true;
        }

        return matchedAnyLine ? lineTo : -1;
    }

    private static int skipEmptySourceLines(List<String> normalizedSource, int sourceIndex) {
        while (sourceIndex < normalizedSource.size() && normalizedSource.get(sourceIndex).isEmpty()) {
            sourceIndex++;
        }
        return sourceIndex;
    }


    private static List<String> normalizeLines(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
                result.add(line.trim().replaceAll("\\s+", " "));
        }
        return result;
    }


    private static boolean linesSimilar(String a, String b) {
        // Simple check: case and whitespace normalized equality
        return a.equalsIgnoreCase(b);
        // Advanced option: use Levenshtein distance or similar metric here
    }
}
