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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.utils.FileUtils;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.StringUtil;

/**
 * Computes the version 1 evidence fingerprint of an issue: a hash of the customer source code around every
 * trace location, independent of file paths, line numbers, prompts and models. The algorithm is specified in
 * {@code EVIDENCE_HASH_SPEC.md}; every client must produce identical values for identical source.
 */
public final class EvidenceFingerprinter {
    public static final String VERSION_PREFIX = "v1:";
    public static final int WINDOW = 25;
    private static final char BOM = '\uFEFF';

    private final FprHandle fprHandle;
    private final FileUtils fileUtils;

    public EvidenceFingerprinter(FprHandle fprHandle, FileUtils fileUtils) {
        this.fprHandle = fprHandle;
        this.fileUtils = fileUtils;
    }

    public static boolean isCurrentVersion(String fingerprint) {
        return !StringUtil.isEmpty(fingerprint) && fingerprint.startsWith(VERSION_PREFIX);
    }

    /**
     * @return the fingerprint, or empty when there is no usable source evidence: no trace, no trace file in the
     *         source archive, or a trace file that is in the archive but cannot be read.
     */
    public Optional<String> compute(List<List<StackTraceElement>> stackTraces) {
        if (stackTraces == null || stackTraces.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder canonical = new StringBuilder();
        int[] elementCount = {0};
        for (List<StackTraceElement> trace : stackTraces) {
            canonical.append("T\n");
            if (trace != null && !appendElements(trace, canonical, elementCount)) {
                return Optional.empty();
            }
        }
        if (elementCount[0] == 0) {
            return Optional.empty();
        }
        return Optional.of(VERSION_PREFIX + sha256Hex(canonical.toString()));
    }

    private boolean appendElements(List<StackTraceElement> elements, StringBuilder out, int[] elementCount) {
        for (StackTraceElement element : elements) {
            if (element == null || !isInSourceArchive(element.getFilename())) {
                continue;
            }
            Optional<List<String>> lines = fileUtils.readSourceLines(fprHandle, element.getFilename());
            if (lines.isEmpty()) {
                return false;
            }
            elementCount[0]++;
            out.append("E\n");
            appendWindow(lines.get(), element.getLine(), out);
            if (!element.getInnerStackTrace().isEmpty()) {
                out.append("I\n");
                if (!appendElements(element.getInnerStackTrace(), out, elementCount)) {
                    return false;
                }
                out.append("/I\n");
            }
        }
        return true;
    }

    private boolean isInSourceArchive(String filename) {
        return !StringUtil.isEmpty(filename) && fprHandle.getSourceFileMap().containsKey(filename);
    }

    private static void appendWindow(List<String> lines, int line, StringBuilder out) {
        if (line <= 0) {
            out.append("-\n");
            return;
        }
        int from = Math.max(1, line - WINDOW);
        int to = Math.min(lines.size(), line + WINDOW);
        for (int i = from; i <= to; i++) {
            String text = lines.get(i - 1);
            if (i == 1 && !text.isEmpty() && text.charAt(0) == BOM) {
                text = text.substring(1);
            }
            out.append(stripTrailingWhitespace(text)).append('\n');
        }
    }

    private static String stripTrailingWhitespace(String text) {
        int end = text.length();
        while (end > 0 && isTrailingWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    private static boolean isTrailingWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\u000B' || c == '\f' || c == '\r';
    }

    private static String sha256Hex(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
