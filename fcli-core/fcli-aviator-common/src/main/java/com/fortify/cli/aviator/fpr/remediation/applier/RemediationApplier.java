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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.fpr.remediation.SkipReason;
import com.fortify.cli.aviator.fpr.remediation.classifier.AppliedChangeLedger;
import com.fortify.cli.aviator.fpr.remediation.exception.SkipRemediationException;
import com.fortify.cli.aviator.fpr.remediation.model.AppliedChange;
import com.fortify.cli.aviator.fpr.remediation.model.Hunk;
import com.fortify.cli.aviator.util.FileUtil;

/** Houses the original {@code applyChange}, split into the same steps it always tried, just named. */
public final class RemediationApplier {
    private static final Logger LOG = LoggerFactory.getLogger(RemediationApplier.class);

    private final FuzzyAnchorLocator fuzzyAnchorLocator = new FuzzyAnchorLocator();

    public String applyChange(String instanceId, String filename, Path filePath, String fileHash, Charset sourceEncoding,
            String originalContent, Hunk hunk, int changeIndex, AppliedChangeLedger ledger) {
        String lineSeparator = detectLineSeparator(originalContent);
        String content = normalizeLineEndings(originalContent);

        List<String> originalLines = Arrays.asList(content.split("\n", -1));
        LOG.debug("Decoded '{}' using {}; lineSeparator={}, normalizedLines={}", filename, sourceEncoding.name(),
                describeLineSeparator(lineSeparator), originalLines.size());

        int lineFrom = hunk.lineFrom();
        int lineTo = hunk.lineTo();
        LOG.debug("Remediation {} change {} for '{}' targets lines {}-{}", instanceId, changeIndex, filename, lineFrom, lineTo);

        boolean fileHashMatches = tryHashMatch(instanceId, filename, fileHash, sourceEncoding, content, originalContent);
        if (!fileHashMatches) {
            LOG.debug("File hash mismatch for remediation {} in {}; searching changed source content", instanceId, filename);
            List<AppliedChange> priorApplied = ledger.changesFor(filePath);

            int[] projected = tryOffsetProjection(instanceId, filename, hunk, originalLines, lineFrom, lineTo, priorApplied, ledger, filePath);
            if (projected != null) {
                lineFrom = projected[0];
                lineTo = projected[1];
            } else {
                int[] anchored = tryFuzzyAnchor(instanceId, filename, hunk, originalLines, priorApplied,
                    lineFrom, lineTo, filePath, ledger);
                lineFrom = anchored[0];
                lineTo = anchored[1];
            }
        }

        validateLineRange(lineFrom, lineTo, originalLines.size(), filename);
        List<String> newCodeLines = new ArrayList<>(Arrays.asList(FileUtil.stripSyntheticLineMarkers(
            hunk.requiredNewCode(), filename).split("\n")));
        dropDuplicatedBoundaryTokens(newCodeLines, originalLines, lineFrom, lineTo, instanceId, filename);
        List<String> updatedLines = new ArrayList<>();
        updatedLines.addAll(originalLines.subList(0, lineFrom - 1));
        updatedLines.addAll(newCodeLines);
        updatedLines.addAll(originalLines.subList(lineTo, originalLines.size()));
        LOG.debug("Staged remediation {} change {} for '{}' using FVDL encoding {}; updatedLines={}", instanceId, changeIndex,
                filename, sourceEncoding.name(), updatedLines.size());
        return String.join(lineSeparator, updatedLines);
    }

    /**
     * Try canonical hash first (matches the new AuditProcessor form), then legacy raw-content
     * hash so pre-fix FPRs still match. Try each with BOTH UTF-8 and the file's declared source
     * encoding — the doc's "5 of 53 files not valid UTF-8" case fails when audit and apply disagree
     * on the encoding used for the getBytes step; accepting the source-encoding form covers it.
     */
    private boolean tryHashMatch(String instanceId, String filename, String fileHash, Charset sourceEncoding, String content,
            String originalContent) {
        String canonicalStr = FileUtil.canonicalizeForHash(content);
        String legacyStr = originalContent;
        String canonicalHashUtf8 = calculateHashBase64Bytes(canonicalStr.getBytes(StandardCharsets.UTF_8), "SHA-256");
        String legacyHashUtf8 = calculateHashBase64Bytes(legacyStr.getBytes(StandardCharsets.UTF_8), "SHA-256");
        String canonicalHashSrc = calculateHashBase64Bytes(canonicalStr.getBytes(sourceEncoding), "SHA-256");
        String legacyHashSrc = calculateHashBase64Bytes(legacyStr.getBytes(sourceEncoding), "SHA-256");
        boolean fileHashMatches;
        String matchedForm;
        if (canonicalHashUtf8.equals(fileHash)) {
            fileHashMatches = true;
            matchedForm = "canonical";
        } else if (legacyHashUtf8.equals(fileHash)) {
            fileHashMatches = true;
            matchedForm = "legacy";
        } else if (canonicalHashSrc.equals(fileHash)) {
            fileHashMatches = true;
            matchedForm = "canonical/" + sourceEncoding.name();
        } else if (legacyHashSrc.equals(fileHash)) {
            fileHashMatches = true;
            matchedForm = "legacy/" + sourceEncoding.name();
        } else {
            fileHashMatches = false;
            matchedForm = "none";
        }
        LOG.debug("Remediation {} hash check for '{}': {}",
            instanceId, filename, fileHashMatches ? ("matched (" + matchedForm + ")") : "mismatched");
        return fileHashMatches;
    }

    /**
     * If a prior remediation this run modified this file, project the declared range through
     * the accumulated line-delta of every AppliedChange whose original range sits strictly
     * before this hunk's declared start. Verify the projected position holds the expected
     * OriginalCode (whitespace-insensitive). Returns {@code null} (try the fuzzy fallback
     * instead) if there is no prior history, the projected range is out of bounds, or the
     * anchor at the projected position doesn't match.
     */
    private int[] tryOffsetProjection(String instanceId, String filename, Hunk hunk, List<String> originalLines,
            int lineFrom, int lineTo, List<AppliedChange> priorApplied, AppliedChangeLedger ledger, Path filePath) {
        if (priorApplied.isEmpty()) {
            return null;
        }
        int shift = ledger.projectOffset(filePath, lineFrom);
        int projectedFrom = lineFrom + shift;
        int projectedTo = lineTo + shift;
        if (projectedFrom >= 1 && projectedTo >= projectedFrom && projectedTo <= originalLines.size()) {
            String originalCodeText = hunk.requiredOriginalCode();
            List<String> originalCodeLines = Arrays.asList(originalCodeText.split("\\r?\\n"));
            if (linesEqualNormalized(originalLines, projectedFrom - 1, projectedTo - 1, originalCodeLines)) {
                LOG.debug("Remediation {} projected via offset map for '{}': declared {}-{} shifted by {} to {}-{}",
                    instanceId, filename, lineFrom, lineTo, shift, projectedFrom, projectedTo);
                return new int[] {projectedFrom, projectedTo};
            } else {
                LOG.debug("Remediation {} projection anchor mismatch for '{}' at projected {}-{}; falling back",
                    instanceId, filename, projectedFrom, projectedTo);
            }
        }
        return null;
    }

    /** Context search first, then a whole-file OriginalCode fallback if no context match was found. */
    private int[] tryFuzzyAnchor(String instanceId, String filename, Hunk hunk, List<String> originalLines,
            List<AppliedChange> priorApplied, int lineFrom, int lineTo, Path filePath, AppliedChangeLedger ledger) {
        int shift = ledger.projectOffset(filePath, lineFrom);
        int projectedFrom = lineFrom + shift;
        int projectedTo = lineTo + shift;
        String contextText = hunk.requiredContextText();
        List<String> contextLine = Arrays.asList(contextText.split("\\r?\\n"));
        int contextBefore = hunk.contextBefore();
        int contextAfter = hunk.contextAfter();
        int contextLineFrom = fuzzyAnchorLocator.searchContext(instanceId, filename, originalLines, contextLine,
            projectedFrom, contextBefore);
        if (contextLineFrom == -1) {
            LOG.debug("Context search failed for remediation {} in {}; trying whole-file OriginalCode fallback",
                instanceId, filename);
            String fallbackOriginalCodeText = hunk.requiredOriginalCode();
            List<String> fallbackOriginalCodeLine = Arrays.asList(fallbackOriginalCodeText.split("\\r?\\n"));
            int[] wholeFile = fuzzyAnchorLocator.searchOriginalCode(instanceId, filename, originalLines, fallbackOriginalCodeLine,
                0, originalLines.size(), 0, 0, projectedFrom, projectedTo);
            if (wholeFile[0] != -1 && wholeFile[1] != -1) {
                LOG.debug("Whole-file OriginalCode fallback matched remediation {} in {} at lines {}-{}",
                    instanceId, filename, wholeFile[0] + 1, wholeFile[1] + 1);
                return new int[] {wholeFile[0] + 1, wholeFile[1] + 1};
            } else {
                LOG.debug("Whole-file OriginalCode fallback failed for remediation {} in {}", instanceId, filename);
                SkipReason failureReason = priorApplied.isEmpty()
                    ? SkipReason.SOURCE_CONTEXT_NOT_FOUND
                    : SkipReason.ANCHOR_DOES_NOT_MATCH;
                throw new SkipRemediationException(failureReason, "Anchor not found for file '" + filename +
                    "'; " + (priorApplied.isEmpty()
                        ? "file may have changed on disk or context is missing"
                        : "prior remediation shifted or rewrote the anchor lines this run"));
            }
        } else {
            LOG.debug("Context for remediation {} in {} matched at line {}", instanceId, filename, contextLineFrom + 1);
            String originalCodeText = hunk.requiredOriginalCode();
            List<String> originalCodeLine = Arrays.asList(originalCodeText.split("\\r?\\n"));
            int[] lineFromTo = fuzzyAnchorLocator.searchOriginalCode(instanceId, filename, originalLines, originalCodeLine,
                contextLineFrom, contextLine.size(), contextBefore, contextAfter, projectedFrom, projectedTo);
            if (lineFromTo[0] == -1 || lineFromTo[1] == -1) {
                LOG.debug("Original code search failed for remediation {} in {}; context line={}, original code lines={}, source lines={}",
                    instanceId, filename, contextLineFrom + 1, originalCodeLine.size(), originalLines.size());
                SkipReason failureReason = priorApplied.isEmpty()
                    ? SkipReason.ORIGINAL_CODE_NOT_FOUND
                    : SkipReason.ANCHOR_DOES_NOT_MATCH;
                throw new SkipRemediationException(failureReason, "Original code not found for file '" + filename +
                    "'; " + (priorApplied.isEmpty()
                        ? "file may have changed on disk"
                        : "prior remediation altered lines inside this hunk's context window"));
            }
            int resultLineFrom = lineFromTo[0] + 1;
            int resultLineTo = lineFromTo[1] + 1;
            LOG.debug("Original code for remediation {} in {} matched at lines {}-{}", instanceId, filename, resultLineFrom, resultLineTo);
            return new int[] {resultLineFrom, resultLineTo};
        }
    }

    private void dropDuplicatedBoundaryTokens(List<String> newCodeLines, List<String> originalLines,
            int lineFrom, int lineTo, String instanceId, String filename) {
        if (newCodeLines.isEmpty()) return;
        if (lineFrom > 1 && newCodeLines.size() > 1) {
            String lineBefore = originalLines.get(lineFrom - 2);
            if (boundaryLinesMatch(lineBefore, newCodeLines.get(0))) {
                LOG.debug("Remediation {} for '{}': dropping duplicated leading boundary token in NewCode (matches line {})",
                    instanceId, filename, lineFrom - 1);
                newCodeLines.remove(0);
            }
        }
        if (lineTo < originalLines.size() && newCodeLines.size() > 1) {
            String lineAfter = originalLines.get(lineTo);
            if (boundaryLinesMatch(lineAfter, newCodeLines.get(newCodeLines.size() - 1))) {
                LOG.debug("Remediation {} for '{}': dropping duplicated trailing boundary token in NewCode (matches line {})",
                    instanceId, filename, lineTo + 1);
                newCodeLines.remove(newCodeLines.size() - 1);
            }
        }
    }

    private boolean boundaryLinesMatch(String a, String b) {
        if (a == null || b == null) return false;
        String normA = a.trim().replaceAll("\\s+", " ");
        String normB = b.trim().replaceAll("\\s+", " ");
        return !normA.isEmpty() && normA.equals(normB);
    }

    private void validateLineRange(int lineFrom, int lineTo, int sourceLineCount, String filename) {
        if (lineFrom < 1 || lineTo < lineFrom || lineTo > sourceLineCount) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_LINE_RANGE_INVALID,
                    "Invalid remediation line range " + lineFrom + "-" + lineTo + " for file '" + filename + "'");
        }
    }

    /**
     * Anchor verification: line-by-line whitespace-insensitive, case-insensitive comparison
     * between a slice of the current file and the expected OriginalCode. Matches the same
     * normalization ({@code trim().replaceAll("\\s+", " ")}, {@code equalsIgnoreCase}) that
     * {@link com.fortify.cli.aviator.util.FuzzyContextSearcher} uses so behaviour is consistent
     * between the fast projection path and the fuzzy fallback.
     */
    private boolean linesEqualNormalized(List<String> source, int startInclusive, int endInclusive, List<String> expected) {
        int len = endInclusive - startInclusive + 1;
        if (len != expected.size()) return false;
        for (int i = 0; i < len; i++) {
            String a = source.get(startInclusive + i).trim().replaceAll("\\s+", " ");
            String b = expected.get(i).trim().replaceAll("\\s+", " ");
            if (!a.equalsIgnoreCase(b)) return false;
        }
        return true;
    }

    private String detectLineSeparator(String content) {
        int crlfIndex = content.indexOf("\r\n");
        int lfIndex = content.indexOf('\n');
        int crIndex = content.indexOf('\r');

        if (crlfIndex >= 0 && (lfIndex == crlfIndex + 1 || lfIndex < 0) && (crIndex == crlfIndex || crIndex < 0)) {
            return "\r\n";
        }
        if (lfIndex >= 0 && (crIndex < 0 || lfIndex < crIndex)) {
            return "\n";
        }
        if (crIndex >= 0) {
            return "\r";
        }
        return System.lineSeparator();
    }

    private String normalizeLineEndings(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String describeLineSeparator(String lineSeparator) {
        return switch (lineSeparator) {
            case "\r\n" -> "CRLF";
            case "\n" -> "LF";
            case "\r" -> "CR";
            default -> "system";
        };
    }

    private String calculateHashBase64(String content, String algorithm) {
        String hash;
        if (content == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            hash = Base64.getEncoder().encodeToString(digest);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new AviatorTechnicalException("Hashing algorithm not available: " + algorithm, e);
        }
    }

    /** Encoding-agnostic hash — caller supplies the already-encoded bytes. */
    private String calculateHashBase64Bytes(byte[] bytes, String algorithm) {
        if (bytes == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return Base64.getEncoder().encodeToString(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AviatorTechnicalException("Hashing algorithm not available: " + algorithm, e);
        }
    }
}
