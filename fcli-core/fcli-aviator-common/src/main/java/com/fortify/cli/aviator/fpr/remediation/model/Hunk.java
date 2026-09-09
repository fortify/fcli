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
package com.fortify.cli.aviator.fpr.remediation.model;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fortify.cli.aviator.util.FileTypeLanguageMapperUtil;
import com.fortify.cli.aviator.util.FileUtil;
import com.fortify.cli.aviator.util.LanguageCommentMapperUtil;


public final class Hunk {
    private final String lineFromRaw;
    private final String lineToRaw;
    private final String contextTextRaw;
    private final String contextBeforeRaw;
    private final String contextAfterRaw;
    private final String originalCodeRaw;
    private final String newCodeRaw;

    public Hunk(String lineFromRaw, String lineToRaw, String contextTextRaw, String contextBeforeRaw,
                String contextAfterRaw, String originalCodeRaw, String newCodeRaw) {
        this.lineFromRaw = lineFromRaw;
        this.lineToRaw = lineToRaw;
        this.contextTextRaw = contextTextRaw;
        this.contextBeforeRaw = contextBeforeRaw;
        this.contextAfterRaw = contextAfterRaw;
        this.originalCodeRaw = originalCodeRaw;
        this.newCodeRaw = newCodeRaw;
    }

    public int lineFrom() {
        return RequiredFields.requireInt(lineFromRaw, "LineFrom");
    }

    public int lineTo() {
        return RequiredFields.requireInt(lineToRaw, "LineTo");
    }

    public String requiredContextText() {
        return RequiredFields.requireText(contextTextRaw, "Context");
    }

    public int contextBefore() {
        return RequiredFields.requireContextAttribute(contextBeforeRaw, "before");
    }

    public int contextAfter() {
        return RequiredFields.requireContextAttribute(contextAfterRaw, "after");
    }

    public String requiredOriginalCode() {
        return RequiredFields.requireText(originalCodeRaw, "OriginalCode");
    }

    public String requiredNewCode() {
        return RequiredFields.requireText(newCodeRaw, "NewCode");
    }

    
    public String comparisonCode(String filename) {
        String normalizedCode = normalizeProposedCode(requiredNewCode(), filename);
        return createComparisonCode(normalizedCode, filename);
    }

    private static String normalizeProposedCode(String content, String fileName) {
        if (content == null) return null;

        String language = FileTypeLanguageMapperUtil.getProgrammingLanguage(
            FileUtil.getFileExtension(fileName));
        String commentSymbol = LanguageCommentMapperUtil.getProgrammingLanguageComment(language);

        if ("Unknown".equals(commentSymbol)) return trimBlankLines(content);

        String closingToken = commentSymbol.equals("<!--") ? "-->"
            : commentSymbol.equals("<%--") ? "--%>" : null;

        Pattern markerPattern = Pattern.compile(
            "[ \\t]*" + Pattern.quote(commentSymbol) + " L\\d+"
                + (closingToken != null ? "[ \\t]*" + Pattern.quote(closingToken) : "")
                + "[ \\t]*$");

        String[] lines = content.split("\\R", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = markerPattern.matcher(lines[i]);
            result.append(matcher.find() ? lines[i].substring(0, matcher.start()) : lines[i]);
            if (i < lines.length - 1) result.append(System.lineSeparator());
        }

        return trimBlankLines(result.toString());
    }

    private static String createComparisonCode(String normalizedCode, String fileName) {
        if (normalizedCode == null) return null;

        String language = FileTypeLanguageMapperUtil.getProgrammingLanguage(
            FileUtil.getFileExtension(fileName));
        String commentSymbol = LanguageCommentMapperUtil.getProgrammingLanguageComment(language);

        if ("Unknown".equals(commentSymbol)) {
            return normalizeLiteralAliases(normalizedCode).replaceAll("\\s+", "");
        }

        String comparisonCode = normalizedCode;
        String closingToken = commentSymbol.equals("<!--") ? "-->"
            : commentSymbol.equals("<%--") ? "--%>" : null;

        if (closingToken != null) {
            comparisonCode = comparisonCode.replaceAll(
                "(?s)" + Pattern.quote(commentSymbol) + ".*?" + Pattern.quote(closingToken), "");
        } else if ("//".equals(commentSymbol)) {
            comparisonCode = comparisonCode.replaceAll("(?m)" + Pattern.quote(commentSymbol) + ".*$", "")
                .replaceAll("(?s)/\\*.*?\\*/", "");
        } else if ("#".equals(commentSymbol)) {
            comparisonCode = comparisonCode.replaceAll("(?m)" + Pattern.quote(commentSymbol) + ".*$", "");
        }

        return normalizeLiteralAliases(comparisonCode).replaceAll("\\s+", "");
    }

    /**
     * Treats semantically-equivalent literal forms as identical for near-identical-fix
     * comparison only; never applied to code actually written to source files.
     */
    private static String normalizeLiteralAliases(String code) {
        if (code == null) return null;
        String normalized = code.replaceAll("'\\\\0'", "0");
        normalized = normalized.replaceAll("\\bnullptr\\b", "NULL");
        return normalized;
    }

    private static String trimBlankLines(String content) {
        String[] lines = content.split("\\R", -1);
        int start = 0, end = lines.length - 1;

        while (start <= end && lines[start].isBlank()) start++;
        while (end >= start && lines[end].isBlank()) end--;

        return start > end ? "" :
            String.join(System.lineSeparator(), Arrays.copyOfRange(lines, start, end + 1));
    }
}
