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
 * Computes the version 1 evidence fingerprint of an issue: a hash of the customer source code around the frames of
 * its stack traces. It changes only when that code changes. Every client must apply exactly these rules, otherwise
 * an issue audited by one tool would look changed to another and be re-audited for nothing.
 *
 * <p><b>What is hashed.</b> For every frame of every stack trace, the lines of the frame's source file from
 * {@value #WINDOW} lines before the frame's line to {@value #WINDOW} lines after it, clamped to the file. Only these
 * windows are hashed, not the whole file, so a change more than {@value #WINDOW} lines away from every frame does not
 * change the fingerprint and does not trigger a re-audit. File paths, line numbers, prompts and model versions are
 * not hashed, so inserting lines above a frame or upgrading a prompt or model never changes it either.
 *
 * <p><b>Source lines.</b> The file is the decoded text with a leading BOM removed, split on {@code \n} with a
 * {@code \r} dropped before each {@code \n}, and with trailing empty lines dropped. Each line has trailing space,
 * tab, vertical tab, form feed and carriage return characters removed. A frame whose line is 0 or less contributes
 * the single line {@code -} instead of a window.
 *
 * <p><b>Canonical text.</b> The hash input is built from these markers, each written on its own line:
 * <ul>
 * <li>{@code T} starts a stack trace. It is written once per trace, in order, before the trace's frames.</li>
 * <li>{@code E} starts a frame. It is written directly before that frame's window lines. A frame whose file is not
 * in the source archive is skipped together with its inner trace, and writes nothing.</li>
 * <li>{@code I} and {@code /I} wrap the frames of a frame's inner stack trace, which follow the frame's own window
 * lines.</li>
 * </ul>
 * For example, a first trace with two frames, the first of which has one inner frame, and a second trace with one
 * frame:
 * <pre>
 * T
 * E
 * (window lines of frame 1)
 * I
 * E
 * (window lines of the inner frame)
 * /I
 * E
 * (window lines of frame 2)
 * T
 * E
 * (window lines of frame 3)
 * </pre>
 * The fingerprint is {@code v1:} followed by the lowercase hexadecimal SHA-256 of the UTF-8 bytes of this text. The
 * markers keep the boundaries unambiguous: the same lines grouped into different frames or traces give a different
 * fingerprint.
 *
 * <p>If no frame contributes because none has a file in the source archive, or if a source file that is in the
 * archive cannot be read, there is no fingerprint and the issue is neither baselined nor re-audited.
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
