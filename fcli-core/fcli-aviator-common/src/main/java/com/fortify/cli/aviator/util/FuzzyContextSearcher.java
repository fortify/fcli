/*
 * Copyright 2021-2025 Open Text.
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

    public static int[] fuzzySearchOriginalCode(List<String> sourceLines, List<String> originalCodeLine, int maxMismatches, int startIndex){
        List<String> normalizedSource = normalizeLines(sourceLines);
        List<String> normalizedOriginalCode = normalizeLines(originalCodeLine);
        int[] lineFromTo = new int[2]; //0th index represents original line starting line number in sourceLine
        //1st index represents original line end line number in sourceLine
        int misMatches;
        int sourceIndex;
        for(int i=startIndex; i<normalizedSource.size(); i++)
        {
            misMatches=0;
            sourceIndex = i;
            if(normalizedSource.get(i).isEmpty())
                continue;
            for(int j=0; j<normalizedOriginalCode.size(); j++){
                if(normalizedOriginalCode.get(j).isEmpty())
                    continue;
                if( j>0 && normalizedSource.get(sourceIndex).isEmpty())
                {
                    sourceIndex++;
                    //continue;
                }
                if(!linesSimilar(normalizedSource.get(sourceIndex), normalizedOriginalCode.get(j)))
                {
                    misMatches++;
                    if(misMatches>maxMismatches)
                        break;
                }
                if(j==normalizedOriginalCode.size()-1)
                {
                    lineFromTo[0] = i;
                    lineFromTo[1] = sourceIndex;
                    return lineFromTo;
                }
                sourceIndex++;
            }

        }
        //Original Code lines not found in source Code
        lineFromTo[0] = -1;
        lineFromTo[1] = -1;
        return lineFromTo;
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
