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

import java.util.List;

/**
 * Counts the opening and closing delimiters (braces, parentheses, brackets) in lines of code, ignoring those inside string
 * and character literals and comments. A replacement that keeps the delimiter balance of the lines it replaces cannot
 * unbalance the code around it, which is what a line made only of delimiters, such as {@code }}, needs to be judged by:
 * the same line at a different indentation closes a different block, so comparing its text proves nothing.
 */
final class DelimiterBalance {
    private static final String OPENING = "{([";
    private static final String CLOSING = "})]";

    private DelimiterBalance() {
    }

    /** Net count of {@code {}}, {@code ()} and {@code []}, in that order: openings minus closings. */
    static int[] of(List<String> lines) {
        int[] balance = new int[3];
        boolean inBlockComment = false;
        for (String line : lines) {
            inBlockComment = count(line, balance, inBlockComment);
        }
        return balance;
    }

    static int[] of(String line) {
        int[] balance = new int[3];
        count(line, balance, false);
        return balance;
    }

    /** True for a line that is nothing but delimiters, separators and whitespace, like {@code }} or {@code });}. */
    static boolean isDelimitersOnly(String line) {
        String trimmed = line == null ? "" : line.trim();
        return !trimmed.isEmpty() && trimmed.chars().allMatch(c -> OPENING.indexOf(c) >= 0 || CLOSING.indexOf(c) >= 0
                || c == ';' || c == ',' || Character.isWhitespace(c));
    }

    static boolean isBalanced(int[] balance) {
        return balance[0] == 0 && balance[1] == 0 && balance[2] == 0;
    }

    private static boolean count(String line, int[] balance, boolean inBlockComment) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inBlockComment) {
                if (c == '*' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i++;
                }
            } else if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                return inBlockComment;
            } else if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '*') {
                inBlockComment = true;
                i++;
            } else {
                int opening = OPENING.indexOf(c);
                if (opening >= 0) {
                    balance[opening]++;
                }
                int closing = CLOSING.indexOf(c);
                if (closing >= 0) {
                    balance[closing]--;
                }
            }
        }
        return inBlockComment;
    }
}
