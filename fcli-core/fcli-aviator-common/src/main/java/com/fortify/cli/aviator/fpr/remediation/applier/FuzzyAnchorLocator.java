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
package com.fortify.cli.aviator.fpr.remediation.applier;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fortify.cli.aviator.fpr.remediation.SkipReason;
import com.fortify.cli.aviator.fpr.remediation.exception.SkipRemediationException;
import com.fortify.cli.aviator.util.FuzzyContextSearcher;

/** Wraps the {@link FuzzyContextSearcher} utility's context/original-code matching, unmodified from the original. */
public final class FuzzyAnchorLocator {

    public int searchContext(String instanceId, String filename, List<String> originalLines, List<String> contextLine,
            int projectedDeclaredFrom, int contextBefore) {
        try {
            List<Integer> matches = FuzzyContextSearcher.fuzzySearchContextMatches(originalLines, contextLine, 0);
            if (matches.size() > 1) {
                int expectedContextLineFrom = projectedDeclaredFrom - 1 - contextBefore;
                List<Integer> exact = matches.stream().filter(m -> m == expectedContextLineFrom).toList();
                if (exact.size() == 1) {
                    return exact.get(0);
                }
                String candidateLines = matches.stream()
                        .map(line -> String.valueOf(line + 1))
                        .collect(Collectors.joining(", "));
                throw new SkipRemediationException(SkipReason.SOURCE_CONTEXT_AMBIGUOUS,
                        "Source context matched multiple locations in file '" + filename + "'; candidate lines: " + candidateLines);
            }
            return matches.isEmpty() ? -1 : matches.get(0);
        } catch (IOException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_CONTEXT_NOT_FOUND,
                    "Error searching source context for remediation '" + instanceId + "' in file '" + filename + "'", e);
        }
    }

    public int[] searchOriginalCode(String instanceId, String filename, List<String> originalLines, List<String> originalCodeLine,
            int contextLineFrom, int contextLineCount, int contextBefore, int contextAfter,
            int projectedDeclaredFrom, int projectedDeclaredTo) {
        int contextStart = contextLineFrom + contextBefore;
        int contextEnd = contextLineFrom + contextLineCount - contextAfter;
        if (contextStart < 0 || contextStart >= contextEnd || contextEnd > originalLines.size()) {
            return new int[] {-1, -1};
        }

        List<int[]> matches = FuzzyContextSearcher.fuzzySearchOriginalCodeMatches(
                originalLines.subList(contextStart, contextEnd), originalCodeLine, 0, 0);
        if (matches.size() > 1) {
            int expectedFrom = projectedDeclaredFrom - 1 - contextStart;
            int expectedTo = projectedDeclaredTo - 1 - contextStart;
            List<int[]> exact = matches.stream().filter(m -> m[0] == expectedFrom && m[1] == expectedTo).toList();
            if (exact.size() == 1) {
                int[] m = exact.get(0);
                return new int[] {m[0] + contextStart, m[1] + contextStart};
            }
            String candidateLines = matches.stream()
                    .map(m -> String.valueOf(m[0] + contextStart + 1))
                    .collect(Collectors.joining(", "));
            throw new SkipRemediationException(SkipReason.ORIGINAL_CODE_AMBIGUOUS,
                    "Original code matched multiple locations in file '" + filename + "'; candidate lines: " + candidateLines);
        }
        if (matches.isEmpty()) {
            return new int[] {-1, -1};
        }
        int[] lineFromTo = matches.get(0);
        return new int[] {lineFromTo[0] + contextStart, lineFromTo[1] + contextStart};
    }
}
